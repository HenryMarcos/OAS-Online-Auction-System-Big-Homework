package com.groupproject.client.utils;

import java.util.List;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.user.User;

public class SessionManager {
    private static SessionManager instance;

    private static User currentUser = null;
    private static List<Category> currentCategories;

    // THÊM AUCTION STATE VÀO ĐÂY 
    private static List<Auction> activeAuctions;
    private static List<Auction> myAllAuctions;
    private static List<Auction> allAuctions;
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
        myAllAuctions= null;
        allAuctions=null;
        currentAuction=null;
        activeAuctions= null;
        System.out.println("Session cleared: User logged out.");
    }

    public void setCurrentCategories(List<Category> categories) {
        currentCategories = categories;
        System.out.println("Session updated: Categories is just updated.");
    }

    public List<Category> getCurrentCategories() { return currentCategories; }

    // SETTER AND GETTER
    public void setMyAuctions(List<Auction> auctions) {
        this.myAllAuctions= auctions;
    }
    public void setAllAuctions(List<Auction> auctions) {
        this.allAuctions =  auctions;
    }
    public void setCurrentAuction(Auction auction) {
        this.currentAuction= auction;
    }
    public void setActiveAuction(List<Auction> auctions) {
        this.activeAuctions= auctions;
    }
    public List<Auction> getActiveAuctions() {
        return activeAuctions;
    }
    public List<Auction> getMyAuctions() {
        return myAllAuctions;
    }
    public List<Auction> getAllAuctions() {
        return allAuctions;
    }
    public Auction getCurrentAuction() {
        return currentAuction;
    }
}
