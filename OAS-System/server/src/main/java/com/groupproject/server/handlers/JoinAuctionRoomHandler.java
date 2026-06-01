package com.groupproject.server.handlers;

import java.io.ObjectOutputStream;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.AuctionDAO; // Cần import DAO để lấy dữ liệu
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction; // Object chứa thông tin phiên đấu giá
import com.groupproject.shared.network.request.JoinAuctionRoomRequest;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.JoinAuctionRoomResponse;
import com.groupproject.shared.network.response.Response;

public class JoinAuctionRoomHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        // 1. Kiểm tra tính hợp lệ của Request
        if (!(request instanceof JoinAuctionRoomRequest)) {
            return new JoinAuctionRoomResponse(false, "Invalid request format", null);
        }

        JoinAuctionRoomRequest joinReq = (JoinAuctionRoomRequest) request;
        int auctionId = joinReq.getAuctionId();

        // 2. Móc 'out' từ ClientContext ra để quản lý Pub/Sub
        ObjectOutputStream out = ClientContext.currentOut.get();

        if (out != null) {
            // A. ĐĂNG KÝ NHẬN TIN (DELTA): 
            // Client bắt đầu lắng nghe các NewBidEvent từ giây phút này.
            ClientManager.getInstance().subscribeToAuction(auctionId, out);

            // B. LẤY SNAPSHOT: 
            // Truy vấn trạng thái hiện tại từ Database (Giá hiện tại, người giữ giá, thời gian còn lại...)
            Auction currentAuction = AuctionDAO.getAuctionById(auctionId);

            // Kiểm tra xem phòng đấu giá có tồn tại hay không
            if (currentAuction == null) {
                ServerLogger.error("Auction room not found: " + auctionId);
                return new JoinAuctionRoomResponse(false, "Auction room not found.", null);
            }

            ServerLogger.info("Client successfully joined and synced auction: " + auctionId);
            
            // 3. Trả lời Client kèm theo "Bức ảnh" trạng thái hiện tại (Snapshot)
            return new JoinAuctionRoomResponse(true, "Joined room " + auctionId + " successfully.", currentAuction);
        } else {
            ServerLogger.error("Could not find Output Stream for Client in Context.");
            return new JoinAuctionRoomResponse(false, "Internal Server Error: No Output Stream found.", null);
        }
    }
}