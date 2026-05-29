package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.BidDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.event.NewBidEvent;
import com.groupproject.shared.network.request.PlaceBidRequest;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.PlaceBidResponse;
import com.groupproject.shared.network.response.Response; 

public class PlaceBidHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        
        if (!(request instanceof PlaceBidRequest)) { 
            return new PlaceBidResponse(false, "Invalid request type.");
        }
        
        PlaceBidRequest bidReq = (PlaceBidRequest) request;
        int auctionId = bidReq.getAuctionId();
        double bidAmount = bidReq.getBidAmount();
        int bidderId = ClientContext.currentUser.get().getId();

        // 1. Lấy object Auction từ DB ra để làm "Chìa khóa" (Lock)
        Auction auction = AuctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            return new PlaceBidResponse(false, "Auction not found or ended.");
        }

        // 2. ĐỒNG BỘ HÓA (THREAD-SAFE): Chỉ 1 người được xử lý bid trên phòng này tại 1 thời điểm
        synchronized (auction) {
            
            // Nếu trong lúc xếp hàng chờ, phiên đấu giá đã đóng trên RAM thì loại luôn, không cần gọi DB
            if (!auction.getStatus().equals("ACTIVED")) {
                return new PlaceBidResponse(false, "Auction is no longer active.");
            }

            // 3. Thao tác với DB (DB cũng đã được bảo vệ bằng FOR UPDATE)
            boolean dbSuccess = BidDAO.insertBid(bidReq);
            
            if (dbSuccess) {
                // 4. Cập nhật RAM ngay lập tức, đảm bảo đồng nhất 100% với DB
                // Không cần synchronized ở method updateRamAfterBid nữa vì khối này đã khóa rồi
                AuctionManager.getInstance().updateRamAfterBid(auctionId, bidderId, bidAmount);
                
                ServerLogger.info("Bid placed successfully for Auction " + auctionId);
                
                // 5. Broadcast cho mọi người
                NewBidEvent broadcastEvent = new NewBidEvent(auctionId, bidAmount);
                ClientManager.getInstance().broadcastEventToAuction(auctionId, broadcastEvent);

                return new PlaceBidResponse(true, "Bid successful!");
            } else {
                return new PlaceBidResponse(false, "Bid failed (Invalid amount, insufficient balance, or auction ended).");
            }
        } // Kết thúc vùng an toàn (Release Lock)
    }
}