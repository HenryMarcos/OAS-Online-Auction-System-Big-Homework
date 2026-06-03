package com.groupproject.server.handlers;

import java.util.List;
import java.util.stream.Collectors;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.GetAuctionRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.GetAuctionResponse;
import com.groupproject.shared.network.responses.Response;

public class GetAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        if (!(request instanceof GetAuctionRequest)) {
            return new GetAuctionResponse(false, "Invalid request type.", null);
        }

        GetAuctionRequest req = (GetAuctionRequest) request;
        List<Auction> auctions;

        if (req.getSellerId() != null) {
            // My Auctions: lấy theo seller (mọi trạng thái)
            auctions = AuctionDAO.getAuctionsBySellerId(req.getSellerId(), req.getCategoryId());
            ServerLogger.info("GetAuctionHandler: fetched " + auctions.size() + " auctions for seller " + req.getSellerId());
            
        } else if (req.getStatus() != null) {
            // Lấy theo status (Home screen dùng)
            auctions = AuctionDAO.getAuctionsByStatus(req.getStatus());
            
            // 🌟 FIX: Apply Category filter if the client requested a specific category!
            if (req.getCategoryId() != null) {
                auctions = auctions.stream()
                                   .filter(a -> a.getCategory().getId() == req.getCategoryId())
                                   .collect(Collectors.toList());
            }
            ServerLogger.info("GetAuctionHandler: fetched " + auctions.size() + " auctions with status " + req.getStatus());
            
        } else {
            // Lấy tất cả (Admin)
            auctions = AuctionDAO.getAuctions();
            
            // Apply category filter here too just in case
            if (req.getCategoryId() != null) {
                auctions = auctions.stream()
                                   .filter(a -> a.getCategory().getId() == req.getCategoryId())
                                   .collect(Collectors.toList());
            }
            ServerLogger.info("GetAuctionHandler: fetched all " + auctions.size() + " auctions");
        }

        return new GetAuctionResponse(true, "Success", auctions);
    }
}