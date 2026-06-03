package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.JoinAuctionResponse;
import com.groupproject.shared.network.responses.Response;

public class JoinAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        if (!(request instanceof JoinAuctionRequest)) return new JoinAuctionResponse(false, "Invalid Request");

        int targetId = ((JoinAuctionRequest) request).getAuctionId();
        ServerLogger.info("User " + clientContext.getAuthenticatedUserId() + " attempting to join auction " + targetId);

        // 🌟 DELEGATE TO MANAGER
        AuctionDetail detail = AuctionManager.INSTANCE.getAuctionDetail(targetId);

        if (detail != null) {
            ServerLogger.info("Successfully joined auction " + targetId);
            return new JoinAuctionResponse(true, detail, "Successfully joined auction");
        } else {
            ServerLogger.error("Failed to join auction " + targetId);
            return new JoinAuctionResponse(false, "Auction not found or no longer active.");
        }
    }
}
