package com.groupproject.server.handlers;

import java.util.List;

import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.Admin;
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

        if (duplicateError != null) {
            return new SignupResponse(false, duplicateError);
        }

        User newlyCreatedUser = UserDAO.registerUser(signupReq);

        if (newlyCreatedUser != null) {
            
            // Phân loại danh sách Auction bằng toán tử instanceof
            List<Auction> auctionList;
            if (newlyCreatedUser instanceof Admin) {
                auctionList = AuctionDAO.getAuctions(); // Admin lấy TẤT CẢ
            } else {
                // Nếu là Bidder hoặc Seller thì lấy những phiên đấu giá ACTIVED, WAITING, SCHEDULED
                auctionList = AuctionDAO.getActiveAuctions();
            }

            // Lấy Category từ RAM Cache
            List<Category> mainCategories = CategoryManager.getInstance().getMainCategories();

            return new SignupResponse(true, newlyCreatedUser, mainCategories, auctionList, "Account successfully created!");
        } else {
            return new SignupResponse(false, "Failed to create account. Please try again later.");
        }
    }
}