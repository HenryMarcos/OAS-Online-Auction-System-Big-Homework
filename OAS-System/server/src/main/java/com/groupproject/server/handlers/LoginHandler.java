package com.groupproject.server.handlers;

import java.time.LocalDateTime;

import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.LoginResponse;
import com.groupproject.shared.network.responses.Response;

public class LoginHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        boolean success;
        if (!(request instanceof LoginRequest)) { 
            success = false; // Nếu không phải loại request phù hợp thì thất bại
            ServerLogger.warning("This request is not LoginRequest but " + request.getClass().getSimpleName());
        }
        else success = UserDAO.checkUser((LoginRequest) request);

        if (success) { 
            ServerLogger.info("Successfully handle " + request.getClass().getSimpleName());
            User loggedInUser = UserDAO.getUser((LoginRequest) request);
            clientContext.setAuthenticatedUserId(loggedInUser.getId());

            ClientManager.INSTANCE.registerUser(loggedInUser.getId(), clientContext.getOut());

            return new LoginResponse(true, loggedInUser, CategoryManager.INSTANCE.getMainCategories(), AuctionManager.INSTANCE.getActiveAuctionList(), LocalDateTime.now(), "Welcome back!"); 
        }
        else { 
            ServerLogger.error("Failed to handle" + request.getClass().getSimpleName());
            return new LoginResponse(false, "Invalid username or password"); 
        }
    }
}
