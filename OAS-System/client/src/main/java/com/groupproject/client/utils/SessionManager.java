package com.groupproject.client.utils;

import java.util.List;

import com.groupproject.client.MainController;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.BidDTO;
import com.groupproject.shared.model.transaction.NotificationDTO;
import com.groupproject.shared.model.user.User;

public enum SessionManager {
    INSTANCE;

    private static User currentUser = null; // Người dùng hiện tại
    private static List<Category> currentCategories; // Các danh mục hiện tại lấy từ server
    private static List<Auction> currentAuctionList; // Danh sách các phiên đấu giá đang hoạt động hiện tại

    private static List<Auction> myProductList; // Danh sách các sản phẩm mà người dùng đã/đang bán
    private static List<Auction> joinedAuctions; // Danh sách các phiên đấu giá mà người dùng đã tham gia

    private Auction currentViewingAuction; // Phiên đấu giá mà người dùng đang tham gia
    private List<BidDTO> currentAuctionBids; // Danh sách các bid của phiên đấu giá mà người dùng đang tham gia

    private List<NotificationDTO> notificationList; // Danh sách các thông báo từ server gửi cho user

    private static MainController currentMainController;

    private SessionManager() {}

    public void setCurrentUser(User user) {
        currentUser = user;
        ClientLogger.info("Session updated: User " + (user != null?user.getUsername(): "Unkown") + " is now logged in.");
    }

    public User getCurrentUser() { return currentUser; }

    public boolean isLoggedIn() { return currentUser != null; }

    public void logout() {
        currentUser = null;
        System.out.println("Session cleared: User logged out.");
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

    public void setCurrentAuctionBids(List<BidDTO> bids) { this.currentAuctionBids = bids; }
    public List<BidDTO> getCurrentAuctionBids() { return currentAuctionBids; }

    public void setMyProductList(List<Auction> myProductList) { 
        this.myProductList = myProductList;
        if (myProductList != null) {
            ClientLogger.info("My Product loaded: " + myProductList.size());
        } else {
            ClientLogger.warning("My Product loaded: 0 (List was null)");
        }
    }
    public List<Auction> getMyProductList() { return myProductList; }    

    public List<NotificationDTO> getNotificationList() { return notificationList; }
    public void setNotificationList(List<NotificationDTO> notificationList) { this.notificationList = notificationList; }

    public MainController getCurrentMainController() { return currentMainController; }
    public void setCurrentMainController(MainController mainController) { this.currentMainController = mainController; }

    
}
