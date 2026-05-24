package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.BidDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
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
        
        // 1. Thao tác duy nhất với DB (Đã bao gồm toàn bộ kiểm tra bảo mật)
        boolean dbSuccess = BidDAO.insertBid(bidReq);
        
        if (dbSuccess) {
            int bidderId = ClientContext.currentUser.get().getId();
            
            // 2. Cập nhật trạng thái In-Memory (RAM) để ScheduledExecutor không bị lỗi nhịp
            AuctionManager.getInstance().updateRamAfterBid(bidReq.getAuctionId(), bidderId, bidReq.getBidAmount());
            
            ServerLogger.info("Bid placed successfully for Auction " + bidReq.getAuctionId());
            
            // 3. BROADCAST TO EVERYONE IN THE ROOM
            NewBidEvent broadcastEvent = new NewBidEvent(bidReq.getAuctionId(), bidReq.getBidAmount());
            ClientManager.getInstance().broadcastEventToAuction(bidReq.getAuctionId(), broadcastEvent);

            // 4. Trả lời Client bấm nút
            return new PlaceBidResponse(true, "Bid successful!");
        } else {
            return new PlaceBidResponse(false, "Bid failed (Invalid amount, insufficient balance, or auction ended).");
        }
    }
}