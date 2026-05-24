package com.groupproject.server.handlers;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.CreateAuctionRequest;
import com.groupproject.shared.network.CreateAuctionResponse;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public class CreateAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        
        // 1. Kiểm tra an toàn kiểu dữ liệu (Ngăn chặn ClassCastException)
        if (!(request instanceof CreateAuctionRequest)) { 
            ServerLogger.error("This request is not CreateAuctionRequest but " + request.getClass().getSimpleName());
            return new CreateAuctionResponse(false, "Invalid request type"); // Dừng và trả về lỗi ngay
        }

        // 2. Ép kiểu an toàn sau khi đã check
        CreateAuctionRequest createRequest = (CreateAuctionRequest) request;

        // 3. Gọi Database (DAO lúc này đã lo toàn bộ việc gán status WAITING hay SCHEDULED)
        Auction newlyCreatedAuction = AuctionDAO.createAuction(createRequest);

        // 4. Xử lý kết quả trả về
        if (newlyCreatedAuction != null) {
            ServerLogger.info("Create auction success. ID: " + newlyCreatedAuction.getId() + " - Status: " + newlyCreatedAuction.getStatus());
            
            // Đăng ký cho AuctionManager. 
            // Manager sẽ tự biết phải làm gì với WAITING và SCHEDULED
            AuctionManager.getInstance().registerAuction(newlyCreatedAuction);
            
            return new CreateAuctionResponse(true, newlyCreatedAuction, "Auction created successfully!");
        } else {
            ServerLogger.error("Failed to create auction in database");
            return new CreateAuctionResponse(false, "Failed to create auction in database");
        }
    }
}