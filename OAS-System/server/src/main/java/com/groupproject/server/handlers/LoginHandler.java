package com.groupproject.server.handlers;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.shared.network.LoginRequest;
import com.groupproject.shared.network.LoginResponse;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public class LoginHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        boolean success;
        if (!(request instanceof LoginRequest)) success = false;
        else success = UserDAO.checkUser((LoginRequest) request);

        if (success) { return new LoginResponse(true, null, CategoryDAO.getCategories(), "Welcome back!"); }
        else { return new LoginResponse(false, "Invalid username or password"); }
    }
}
