package com.groupproject.server.handlers;

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
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        boolean success;
        if (!(request instanceof CreateAuctionRequest)) { 
            success = false; // Nếu không phải loại request phù hợp thì thất bại
            ServerLogger.info("This request is not CreateAuctionRequest but " + request.getClass().getSimpleName());
            ServerLogger.error("Failed to create auction");
            return new CreateAuctionResponse(false, "Failed to create auction");
        }

        Auction newlyCreatedAuction = AuctionDAO.createAuction((CreateAuctionRequest) request);

        success = (newlyCreatedAuction != null);

        if (success) {
            ServerLogger.info("Create auction success");
            // Đăng ký auction cho AuctionManager để quản lý thời gian trước khi kết thúc phiên đấu giá
            AuctionManager.INSTANCE.registerAuction(newlyCreatedAuction);
            
            return new CreateAuctionResponse(true, newlyCreatedAuction, null);
        }
        else {
            ServerLogger.error("Failed to create auction");
            return new CreateAuctionResponse(false, "Failed to create auction");
        }
    }
}
