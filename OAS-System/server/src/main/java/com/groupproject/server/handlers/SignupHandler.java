package com.groupproject.server.handlers;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;
import com.groupproject.shared.network.SignupRequest;
import com.groupproject.shared.network.SignupResponse;

public class SignupHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {

        SignupRequest signupReq = (SignupRequest) request;

        String duplicateError = UserDAO.checkDuplicates(signupReq);

        if (duplicateError != null /* Tìm được user trùng dữ liệu */) {
            return new SignupResponse(false, duplicateError);
        }

        User newlyCreatedUser = UserDAO.registerUser(signupReq);

        if (newlyCreatedUser != null) {
            return new SignupResponse(true, newlyCreatedUser, CategoryDAO.getMainCategories(), AuctionDAO.getAuctions(), "Account successfully created!");
        } else {
            return new SignupResponse(false, "Failed to create account. Please try again later.");
        }
    }
}
