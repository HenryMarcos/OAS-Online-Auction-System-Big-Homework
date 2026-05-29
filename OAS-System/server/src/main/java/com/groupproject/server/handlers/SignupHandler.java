package com.groupproject.server.handlers;

import java.time.LocalDateTime;

import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.requests.SignupRequest;
import com.groupproject.shared.network.responses.Response;
import com.groupproject.shared.network.responses.SignupResponse;

public class SignupHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {

        SignupRequest signupReq = (SignupRequest) request;

        String duplicateError = UserDAO.checkDuplicates(signupReq);

        if (duplicateError != null /* Tìm được user trùng dữ liệu */) {
            return new SignupResponse(false, duplicateError);
        }

        User newlyCreatedUser = UserDAO.registerUser(signupReq);

        if (newlyCreatedUser != null) {
            clientContext.setAuthenticatedUserId(newlyCreatedUser.getId());
            ClientManager.INSTANCE.registerUser(newlyCreatedUser.getId(), clientContext.getOut());

            return new SignupResponse(true, newlyCreatedUser, CategoryManager.INSTANCE.getMainCategories(), AuctionManager.INSTANCE.getActiveAuctionList(), LocalDateTime.now(), "Account successfully created!");
        } else {
            return new SignupResponse(false, "Failed to create account. Please try again later.");
        }
    }
}
