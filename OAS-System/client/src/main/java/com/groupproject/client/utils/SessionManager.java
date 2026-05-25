package com.groupproject.client.utils;

import java.util.List;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;

public enum SessionManager {
    INSTANCE;

    private static User currentUser = null;
    private static List<Category> currentCategories;
    private static List<Auction> currentAuctionList;
    private Auction currentViewingAuction;

    private SessionManager() {}

    public void setCurrentUser(User user) {
        currentUser = user;
        ClientLogger.info("Session updated: User " + (user != null?user.getUsername(): "Unkown") + " is now logged in.");
    }

    public User getCurrentUser() { return currentUser; }

    public boolean isLoggedIn() { return currentUser != null; }

    public void logout() {
        currentUser = null;
        ClientLogger.info("Session cleared: User logged out.");
    }

    public void setCurrentCategories(List<Category> categories) {
        currentCategories = categories;
        ClientLogger.info("Session updated: Categories is just updated.");
    }
    public List<Category> getCurrentCategories() { return currentCategories; }

    public void setCurrentAuctionList(List<Auction> auctionList) {
        currentAuctionList = auctionList;
        if (currentAuctionList != null) {
            ClientLogger.info("Auctions loaded: " + currentAuctionList.size());
        } else {
            ClientLogger.warning("Auctions loaded: 0 (List was null)");
        }
    }
    public List<Auction> getCurrentAuctionList() { return currentAuctionList; }

    public void setCurrentViewingAuction(Auction auction) { this.currentViewingAuction = auction; }
    public Auction getCurrentViewingAuction() { return currentViewingAuction; }

}
