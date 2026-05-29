package com.groupproject.client.network;

import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.groupproject.client.ProfileController;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.NotificationStore;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.network.AuctionEvent.AuctionEvent;
import com.groupproject.shared.network.AuctionWonNotification;
import com.groupproject.shared.network.Notification;
import com.groupproject.shared.network.OutBidNotification;
import com.groupproject.shared.network.Wallet;
import com.groupproject.shared.network.events.ServerEvent;
import com.groupproject.shared.network.responses.Response;

import javafx.application.Platform;

public class ServerListener implements Runnable {
    
    @Override
    public void run() {
        ObjectInputStream in = NetworkManager.INSTANCE.getIn();
        ClientLogger.info("Background listener started. Waiting for server...");
        try {
            while (true) { 
                // 1. This line WAITS until the server sends something
                Object incomingData = in.readObject();

                // 2. Make sure it's our standard response object
                if (incomingData instanceof Response || incomingData instanceof ServerEvent) {
                    ClientMessageRouter.INSTANCE.handleIncomingMessage(incomingData);
                } else {
                    ClientLogger.warning("Received unknown object from server.");

                    if (incomingData instanceof Wallet) {
                        Wallet wallet = (Wallet) incomingData;
                        if(wallet.hasWalletUpdated()) {
                            SessionManager.INSTANCE.getCurrentUser().setAccountBalance(wallet.getAvailableBalance());
                            SessionManager.INSTANCE.getCurrentMainController().updateWallet(wallet.getAvailableBalance());
                            ProfileController.getInstance().updateWallet(wallet.getAvailableBalance());
                        }
                    }
                    if (incomingData instanceof AuctionEvent) {
                        AuctionEvent event = (AuctionEvent) incomingData;
                        AuctionEventBus.getInstance().publish(event);
                        // Tạo thêm thông báo ở trong notification ( dùng logger đẻ ghi lại hoặc dùng như cũ để hiển thị)
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
                            int userId = SessionManager.INSTANCE.getCurrentUser().getId().intValue();
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
            }
        } catch (Exception e) {
            // thông báo mất kêt nối với sever tại đây 
            // tạo thêm một cái show Allert 
            ClientLogger.error("Lost connection to the server.");
        }
    }
}