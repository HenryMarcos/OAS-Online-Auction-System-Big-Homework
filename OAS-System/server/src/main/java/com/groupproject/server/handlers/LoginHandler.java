package com.groupproject.server.handlers;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.LoginRequest;
import com.groupproject.shared.network.LoginResponse;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public class LoginHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        boolean success;
        if (!(request instanceof LoginRequest)) { 
            success = false; // Nếu không phải loại request phù hợp thì thất bại
            ServerLogger.info("This request is not LoginRequest but " + request.getClass().getSimpleName());
        }
        else success = UserDAO.checkUser((LoginRequest) request);

        if (success) { 
            ServerLogger.info("Successfully handle " + request.getClass().getSimpleName());
            return new LoginResponse(true, UserDAO.getUser((LoginRequest) request), CategoryDAO.getCategories(), "Welcome back!"); 
        }
        else { 
            ServerLogger.error("Failed to handle" + request.getClass().getSimpleName());
            return new LoginResponse(false, "Invalid username or password"); 
        }
    }
}
