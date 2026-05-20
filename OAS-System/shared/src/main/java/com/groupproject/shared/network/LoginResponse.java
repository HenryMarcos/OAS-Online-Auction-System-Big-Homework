package com.groupproject.shared.network;

import java.util.List;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;

public class LoginResponse extends Response {
    private User user;
    //List<Category> categoryTree = CategoryDAO.getCategories();
    private List<Category> categoryTree;
    private List<Auction> auctionList;

    public LoginResponse(boolean success, User user, List<Category> categoryTree, List<Auction> auctionList, String message) {
        super(success, message);
        this.user = user;
        this.categoryTree = categoryTree;
        this.auctionList = auctionList;
    }

    public LoginResponse(boolean success, String message) {
        super(success, message);
    }

    public User getUser() { return user; }
    public List<Category> getCategoryTree() { return categoryTree; }
    public List<Auction> getAuctionList() { return auctionList; }
}
