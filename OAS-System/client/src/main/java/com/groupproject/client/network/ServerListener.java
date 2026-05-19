package com.groupproject.client.network;

import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.groupproject.client.MainController;
import com.groupproject.client.ProfileController;
import com.groupproject.client.utils.NotificationStore;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.network.Response;
import com.groupproject.shared.network.AuctionEvent.AuctionEvent;
import com.groupproject.shared.network.AuctionWonNotification;
import com.groupproject.shared.network.Notification;
import com.groupproject.shared.network.OutBidNotification;
import com.groupproject.shared.network.Wallet;

import javafx.application.Platform;

public class ServerListener implements Runnable {

    private ObjectInputStream in;

    public ServerListener(ObjectInputStream in) {
        this.in = in;
    }
    @Override
    public void run() {
        System.out.println("Background listener started. Waiting for server...");
        try {
            while (true) { 
                
                // 1. This line WAITS until the server sends something
                Object incomingData = in.readObject();

                // 2. Make sure it's our standard response object

                if (incomingData instanceof Wallet) {
                    Wallet wallet = (Wallet) incomingData;
                    if(wallet.hasWalletUpdated()) {
                        MainController.getInstance().updateWallet(wallet.getAvailableBalance());
                        ProfileController.getInstance().updateWallet(wallet.getAvailableBalance());
                    }
                }
                if (incomingData instanceof Response) {
                    Response response = (Response) incomingData;

                    EventRouter.getInstance().dispatch(response);
                }
                if (incomingData instanceof AuctionEvent) {
                    AuctionEvent event = (AuctionEvent) incomingData;
                    AuctionEventBus.getInstance().publish(event);
                } 
                if (incomingData instanceof OutBidNotification) {
                    OutBidNotification notification = (OutBidNotification) incomingData;
                    Platform.runLater(() -> {
                        String msgText= "BẠN ĐÃ BỊ VƯỢT GIÁ TẠI " + notification.getAuctionId() + ".GIÁ MỚI :" + notification.getNewBidAmount();
                        String time = new SimpleDateFormat("HH:mm").format(new Date(notification.getTimeStamp()));
                        NotificationStore.getInstance().addNotification(new Notification(msgText, time));
                    });
                   
                }
                if (incomingData instanceof AuctionWonNotification) {
                    AuctionWonNotification event = (AuctionWonNotification) incomingData;
                    Platform.runLater(() -> {
                        String msgText="";
                        int userId = SessionManager.getInstance().getCurrentUser().getId().intValue();
                        String time = new SimpleDateFormat("HH:mm").format(new Date(event.getTimeStamp()));
                        if (userId == event.getSellerId()) {
                            msgText = String.format("TIN VUI ! SẢN PHẨM TRONG PHIÊN ĐẤU GIÁ %d ĐÃ ĐƯỢC BÁN THÀNH CÔNG VỚI GIÁ %,.0f USD",event.getAuctionId(),event.getFinalPrice());
                        
                        }
                        else if (userId == event.getHighestBidderId()) {
                            msgText = String.format("CHÚC MỪNG ! BẠN ĐÃ ĐẤU GIÁ ĐƯỢC VẬT PHẨM TRONG PHIÊN %d VỚI MỨC GIÁ %,.0f USD",event.getAuctionId(),event.getFinalPrice());
                        }
                        if (!msgText.isEmpty()) {
                            NotificationStore.getInstance().addNotification(new Notification(msgText, time));
                        } 
                    });
                }
                else {
                    System.out.println("Received unknown object from server.");
                }
            }
        } catch (Exception e) {
            // thông báo mất kêt nối với sever tại đây 
            // tạo thêm một cái show Allert 
        }
    }
}