package com.groupproject.shared.network.responses;

import java.time.LocalDateTime;
import java.util.List;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;

public class SignupResponse extends Response {
    private User user;
    private List<Category> categoryTree;
    private List<Auction> auctionList;
    private LocalDateTime serverTime;

    public SignupResponse(boolean success, User user, List<Category> categoryTree, List<Auction> auctionList, LocalDateTime serverTime, String message) {
        super(success, message);
        this.user = user;
        this.categoryTree = categoryTree;
        this.auctionList = auctionList;
        this.serverTime = serverTime;
    }

    public SignupResponse(boolean success, String message) {
        super(success, message);
    }

    public User getUser() { return user; }
    public List<Category> getCategoryTree() { return categoryTree; }
    public List<Auction> getAuctionList() { return auctionList; }
    public LocalDateTime getServerTime() { return serverTime; }
}
