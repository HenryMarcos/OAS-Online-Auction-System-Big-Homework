package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.BidDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.requests.PlaceBidRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.PlaceBidResponse;
import com.groupproject.shared.network.responses.Response;

public class PlaceBidHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        // 1. Save to database
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        boolean success;
        if (!(request instanceof PlaceBidRequest)) { 
            success = false; // Nếu không phải loại request phù hợp thì thất bại
            ServerLogger.info("This request is not PlaceBidRequest but " + request.getClass().getSimpleName());
        }
        else success = BidDAO.insertBid((PlaceBidRequest) request);
        
        if (success) {
            PlaceBidRequest bidReq = (PlaceBidRequest) request;
            ServerLogger.info("Bid placed successfully for Auction " + bidReq.getAuctionId());
            
            // 2. BROADCAST TO EVERYONE IN THE ROOM
            NewBidEvent broadcastEvent = new NewBidEvent(bidReq.getAuctionId(), bidReq.getBidAmount());
            ClientManager.INSTANCE.broadcastEventToAuction(bidReq.getAuctionId(), broadcastEvent);

            // 3. Reply to the person who clicked the button
            return new PlaceBidResponse(true, "Bid successful!");
        } else {
            return new PlaceBidResponse(false, "Database error while placing bid.");
        }
    }
}
