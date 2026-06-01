package com.groupproject.server.handlers;

import java.util.List;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.BidDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.BidDTO;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.CreateAuctionResponse;
import com.groupproject.shared.network.responses.JoinAuctionResponse;
import com.groupproject.shared.network.responses.Response;

public class JoinAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        if (!(request instanceof JoinAuctionRequest)) { 
            ServerLogger.info("This request is not JoinAuctionRequest but " + request.getClass().getSimpleName());
            return new JoinAuctionResponse(false, "Failed to join auction");
        } 

        JoinAuctionRequest joinReq = (JoinAuctionRequest) request;
        int targetAuctionId = joinReq.getAuctionId();

        // Fetch the active auction from the manager (Ensure you have a getAuction method in AuctionManager)
        Auction targetAuction = AuctionManager.INSTANCE.getAuction(((JoinAuctionRequest) request).getAuctionId());

        if ((targetAuction != null)) {
            ClientManager.INSTANCE.subscribeToAuction(targetAuctionId, clientContext.getOut());

            List<BidDTO> pastBids = BidDAO.getBidsForAuction(targetAuctionId);

            ServerLogger.info("User " + clientContext.getAuthenticatedUserId() + " successfully joined the auction " + ((JoinAuctionRequest) request).getAuctionId());
            return new JoinAuctionResponse(true, targetAuction, pastBids, "Good");
        } else {
            ServerLogger.error("User failed to join auction " + ((JoinAuctionRequest) request).getAuctionId());
            return new CreateAuctionResponse(false, "Failed to create auction");
        }
    }
}
