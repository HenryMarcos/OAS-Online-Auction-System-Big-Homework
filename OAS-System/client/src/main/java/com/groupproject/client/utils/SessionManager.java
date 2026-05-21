package com.groupproject.client.utils;

import java.util.List;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.model.user.User;

public class SessionManager {
    private static SessionManager instance;

    private static User currentUser = null;
    private static List<Category> currentCategories;
    private static AuctionDetail currentAuctionDetail;
    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        currentUser = user;
        System.out.println("Session updated: User " + (user != null ?user.getUsername() :"Unknown") + " is now logged in.");
    }

    public User getCurrentUser() { return currentUser; }

    public boolean isLoggedIn() { return currentUser != null; }

    public void logout() {
        currentUser = null;
        currentAuctionDetail = null;
        System.out.println("Session cleared: User logged out.");
    }

    public void setCurrentCategories(List<Category> categories) {
        currentCategories = categories;
        System.out.println("Session updated: Categories is just updated.");
    }

    public List<Category> getCurrentCategories() { return currentCategories; }

    // SETTER AND GETTER
    public void setCurrentAuctionDetail(AuctionDetail currentAuctionDetail) {
        this.currentAuctionDetail= currentAuctionDetail;
    }
    public AuctionDetail getCurrentAuctionDetail() {
        return currentAuctionDetail;
    }
    
}
