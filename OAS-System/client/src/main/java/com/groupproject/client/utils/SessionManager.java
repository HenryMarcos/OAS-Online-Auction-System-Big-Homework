package com.groupproject.client.utils;

import java.util.List;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;

public class SessionManager {
    private static SessionManager instance;

    private static User currentUser = null;
    private static List<Category> currentCategories;
    private static Auction currentAuction;
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
        currentAuction = null;
        System.out.println("Session cleared: User logged out.");
    }

    public void setCurrentCategories(List<Category> categories) {
        currentCategories = categories;
        System.out.println("Session updated: Categories is just updated.");
    }

    public List<Category> getCurrentCategories() { return currentCategories; }

    // SETTER AND GETTER
    public void setCurrentAuction(Auction currentAuction) {
        this.currentAuction= currentAuction;
    }
    public Auction getCurrentAuction() {
        return currentAuction;
    }
    
}
