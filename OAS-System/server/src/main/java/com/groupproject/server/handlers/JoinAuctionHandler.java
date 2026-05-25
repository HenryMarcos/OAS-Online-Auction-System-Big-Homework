package com.groupproject.server.handlers;

import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.CreateAuctionResponse;
import com.groupproject.shared.network.responses.JoinAuctionResponse;
import com.groupproject.shared.network.responses.Response;

public class JoinAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        boolean success;
        if (!(request instanceof JoinAuctionRequest)) { 
            success = false; // Nếu không phải loại request phù hợp thì thất bại
            ServerLogger.info("This request is not JoinAuctionRequest but " + request.getClass().getSimpleName());
            return new JoinAuctionResponse(false, "Failed to join auction");
        } 

        // Fetch the active auction from the manager (Ensure you have a getAuction method in AuctionManager)
        Auction targetAuction = AuctionManager.INSTANCE.getAuction(((JoinAuctionRequest) request).getAuctionId());

        success = (targetAuction != null);

        if (success) {
            ServerLogger.info("User successfully joined the auction " + ((JoinAuctionRequest) request).getAuctionId());
            return new JoinAuctionResponse(success, targetAuction, "Good");
        } else {
            ServerLogger.error("User failed to join auction " + ((JoinAuctionRequest) request).getAuctionId());
            return new CreateAuctionResponse(false, "Failed to create auction");
        }
    }
}
