package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.requests.UnwatchAuctionRequest;
import com.groupproject.shared.network.responses.Response;
import com.groupproject.shared.network.responses.UnwatchAuctionResponse;

public class UnwatchAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        if (!(request instanceof UnwatchAuctionRequest)) {
            return new UnwatchAuctionResponse(false, "Invalid request type.", -1);
        }

        Integer userId = clientContext.getAuthenticatedUserId();
        if (userId == null) {
            return new UnwatchAuctionResponse(false, "Chưa đăng nhập.", -1);
        }

        UnwatchAuctionRequest unwatchReq = (UnwatchAuctionRequest) request;
        int auctionId = unwatchReq.getAuctionId();

        ClientManager.INSTANCE.unsubscribeToAuction(auctionId, clientContext.getOut());
        ServerLogger.info("User " + userId + " unsubscribed from auction room " + auctionId + ".");
        return new UnwatchAuctionResponse(true, "Hủy đăng ký theo dõi thành công.", auctionId);
    }
}
