package com.groupproject.server.handlers;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.CreateAuctionRequest;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public class CreateAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        boolean success;
        if (!(request instanceof CreateAuctionRequest)) { 
            success = false; // Nếu không phải loại request phù hợp thì thất bại
            ServerLogger.info("This request is not CreateAuctionRequest but " + request.getClass().getSimpleName());
        }

        Auction newlyCreatedAuction = AuctionDAO.createAuction((CreateAuctionRequest) request);

        success = (newlyCreatedAuction != null);

        if (success) {
            ServerLogger.info("Create auction success");
        }
        else {}



        return null;
    }
}
