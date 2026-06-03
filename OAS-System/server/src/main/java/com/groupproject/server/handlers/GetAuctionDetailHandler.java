package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.network.requests.GetAuctionDetailRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.GetAuctionDetailResponse;
import com.groupproject.shared.network.responses.Response;

public class GetAuctionDetailHandler implements RequestHandler {

    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        if (!(request instanceof GetAuctionDetailRequest)) return new GetAuctionDetailResponse(false, "Invalid");
        
        int targetId = ((GetAuctionDetailRequest) request).getAuctionId();
        ServerLogger.info("Fetching Auction Details for Auction ID: " + targetId);

        // 🌟 DELEGATE TO MANAGER
        AuctionDetail detail = AuctionManager.INSTANCE.getAuctionDetail(targetId);

        if (detail != null) {
            return new GetAuctionDetailResponse(true, detail, "Success");
        } else {
            return new GetAuctionDetailResponse(false, "Auction not found or server error.");
        }
    }
}