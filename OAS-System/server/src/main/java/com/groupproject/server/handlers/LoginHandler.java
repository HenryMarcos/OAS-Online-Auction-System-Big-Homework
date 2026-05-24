package com.groupproject.server.handlers;

import java.util.List;

import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.Admin;
import com.groupproject.shared.model.user.User; 
import com.groupproject.shared.network.LoginRequest;
import com.groupproject.shared.network.LoginResponse;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public class LoginHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        
        if (!(request instanceof LoginRequest)) { 
            ServerLogger.info("This request is not LoginRequest but " + request.getClass().getSimpleName());
            return new LoginResponse(false, "Invalid request format");
        }

        LoginRequest loginReq = (LoginRequest) request;
        boolean success = UserDAO.checkUser(loginReq);

        if (success) { 
            ServerLogger.info("Successfully handled " + request.getClass().getSimpleName());
            
            // 1. Lấy thông tin User (Lúc này có thể là Bidder, Seller hoặc Admin)
            User loggedInUser = UserDAO.getUser(loginReq);
            
            // 2. Phân loại danh sách Auction bằng toán tử instanceof
            List<Auction> auctionList;
            if (loggedInUser instanceof Admin) {
                auctionList = AuctionDAO.getAuctions(); // Admin lấy TẤT CẢ
            } else {
                // Nếu là Bidder hoặc Seller thì lấy những phiên đấu giá ACTIVED, WAITING, SCHEDULED
                auctionList = AuctionDAO.getActiveAuctions(); 
            }

            // 3. Tận dụng Cache trên RAM cho Category
            List<Category> mainCategories = CategoryManager.getInstance().getMainCategories();

            return new LoginResponse(true, loggedInUser, mainCategories, auctionList, "Welcome back!"); 
        } else { 
            ServerLogger.error("Failed to authenticate user for " + request.getClass().getSimpleName());
            return new LoginResponse(false, "Invalid username or password"); 
        }
    }
}