package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.dao.AuctionDAO;
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
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        
        if (!(request instanceof CreateAuctionRequest)) { 
            ServerLogger.info("This request is not CreateAuctionRequest but " + request.getClass().getSimpleName());
            ServerLogger.error("Failed to create auction");
            return new CreateAuctionResponse(false, "Failed to create auction");
        }

        CreateAuctionRequest req = (CreateAuctionRequest) request;
    
        // 1. Commit the item structure to the database layout
        Auction newAuction = AuctionDAO.createAuction(
            clientContext.getAuthenticatedUserId(), req.getTitle(), req.getMainImageBytes(), req.getSubImagesBytes(),
            req.getDescription(), req.getCategory(), req.getCategoryGroupedSpecs(),
            req.getStartingPrice(), req.getDuration(), req.getStartTime(), req.getEndTime(), req.getStatus()
        );
        
        if (newAuction != null) {
            // 2. Insert into manager and push live broadcast automatically!
            AuctionManager.INSTANCE.registerNewAuction(newAuction);
            
            ServerLogger.info("Create auction success");
            return new CreateAuctionResponse(true, newAuction, "Auction successfully launched!");
        }

        ServerLogger.error("Failed to create auction");
        return new CreateAuctionResponse(false, "Database validation constraint failed.");
    }
}