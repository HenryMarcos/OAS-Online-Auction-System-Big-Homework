package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.requests.PlaceBidRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.PlaceBidResponse;
import com.groupproject.shared.network.responses.Response;

public class PlaceBidHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        // 1. Save to database
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        if (!(request instanceof PlaceBidRequest)) { 
            ServerLogger.warning("This request is not PlaceBidRequest but " + request.getClass().getSimpleName());
            return new PlaceBidResponse(false, "Invalid Request Type.");
        }

        PlaceBidRequest bidReq = (PlaceBidRequest) request;

        boolean success = AuctionManager.INSTANCE.placeBid(bidReq, clientContext);

        if (success) {
            ServerLogger.info("Bid placed successfully for Auction " + bidReq.getAuctionId());
            
            return new PlaceBidResponse(true, "Bid placed successfully!");
        } else {
            return new PlaceBidResponse(false, "Database error while placing bid.");
        }
    }
}