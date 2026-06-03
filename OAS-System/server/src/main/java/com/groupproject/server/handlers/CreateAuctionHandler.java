package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.CreateAuctionResponse;
import com.groupproject.shared.network.responses.Response;

public class CreateAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        if (!(request instanceof CreateAuctionRequest)) { 
            return new CreateAuctionResponse(false, "Invalid Request Type.");
        }

        CreateAuctionRequest req = (CreateAuctionRequest) request;
        int sellerId = clientContext.getAuthenticatedUserId();
        
        // 🌟 DELEGATE TO THE MANAGER (The Handler only talks to the Manager!)
        Auction newAuction = AuctionManager.INSTANCE.createAuction(req, sellerId);

        if (newAuction != null) {
            ServerLogger.info("Create auction success: ID " + newAuction.getId());
            return new CreateAuctionResponse(true, newAuction, "Auction successfully launched!");
        } else {
            return new CreateAuctionResponse(false, "Failed to create auction. Please check your data.");
        }
    }
}