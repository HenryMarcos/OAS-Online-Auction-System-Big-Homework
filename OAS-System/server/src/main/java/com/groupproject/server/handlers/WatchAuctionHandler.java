package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.requests.WatchAuctionRequest;
import com.groupproject.shared.network.responses.Response;
import com.groupproject.shared.network.responses.WatchAuctionResponse;

public class WatchAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        if (!(request instanceof WatchAuctionRequest)) {
            return new WatchAuctionResponse(false, "Invalid request type.", -1);
        }

        Integer userId = clientContext.getAuthenticatedUserId();
        if (userId == null) {
            return new WatchAuctionResponse(false, "Chưa đăng nhập.", -1);
        }

        WatchAuctionRequest watchReq = (WatchAuctionRequest) request;
        int auctionId = watchReq.getAuctionId();

        Auction auction = AuctionManager.INSTANCE.getAuction(auctionId);
        if (auction == null) {
            return new WatchAuctionResponse(false, "Phiên đấu giá không tồn tại hoặc đã kết thúc.", auctionId);
        }

        ClientManager.INSTANCE.subscribeToAuction(auctionId, clientContext.getOut());
        ServerLogger.info("User " + userId + " subscribed to auction room " + auctionId + " via Watch button.");
        return new WatchAuctionResponse(true, "Đăng ký theo dõi thành công.", auctionId);
    }
}
