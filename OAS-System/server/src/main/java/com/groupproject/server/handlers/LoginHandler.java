package com.groupproject.server.handlers;

import java.time.LocalDateTime;
import java.util.List;

import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.LoginResponse;
import com.groupproject.shared.network.responses.Response;

// lam viec tren 
public class LoginHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        if (!(request instanceof LoginRequest)) { 
            ServerLogger.warning("This request is not LoginRequest but " + request.getClass().getSimpleName());
            return new LoginResponse(false, "Invalid request format");
        }

        LoginRequest loginReq = (LoginRequest) request;
        boolean success = UserDAO.checkUser(loginReq);

        if (success) { 
            ServerLogger.info("Successfully handle " + request.getClass().getSimpleName());
            User loggedInUser = UserDAO.getUser((LoginRequest) request);
            clientContext.setAuthenticatedUserId(loggedInUser.getId());

            ClientManager.INSTANCE.registerUser(loggedInUser.getId(), clientContext.getOut());

            return new LoginResponse(true, loggedInUser, CategoryManager.INSTANCE.getMainCategories(), AuctionManager.INSTANCE.getActiveAuctionList(), LocalDateTime.now(), "Welcome back!"); 
            
            // 1. Lấy thông tin User. 
            // Ở UserDAO.getUser() hệ thống đã tự động check DB admin_list và gán biến isAdmin vào trong User rồi
            ClientContext.currentUser.set(loggedInUser);
            
            // 2. SỬA ĐỔI: Phân loại danh sách Auction bằng thuộc tính isAdmin()
            List<Auction> auctionList;
            if (loggedInUser.isAdmin()) { // <-- Kiểm tra xem User này có phải Admin không
                auctionList = AuctionDAO.getAuctions();
                // Admin lấy TẤT CẢ
            } else {
                // Nếu là Bidder hoặc Seller thì lấy những phiên đấu giá ACTIVED, WAITING, SCHEDULED
                auctionList = AuctionDAO.getActiveAuctions();
            }

            return new LoginResponse(true, loggedInUser, CategoryManager.INSTANCE.getMainCategories(), AuctionManager.INSTANCE.getActiveAuctionList(), LocalDateTime.now(), "Welcome back!"); 
        } else { 
            ServerLogger.error("Failed to authenticate user for " + request.getClass().getSimpleName());
            return new LoginResponse(false, "Invalid username or password"); 
        }
    }
}