package com.groupproject.server.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.groupproject.server.pattern.observer.AuctionObserver;
import com.groupproject.server.pattern.observer.AuctionSubject;

/**
 * Lớp quản lý hệ thống phát thông báo của các phiên đấu giá.
 * LƯU Ý: Hiện tại cấu trúc này đang lưu dữ liệu (danh sách phòng và người quan sát) hoàn toàn trên RAM 
 * (thông qua ConcurrentHashMap). Nếu server khởi động lại, các kết nối/đăng ký nhận thông báo này sẽ bị mất.
 */
public class AuctionNotificationManager implements AuctionSubject {
    
    // 1. KHỞI TẠO LOGGER (Chuẩn SLF4J)
    private static final Logger logger = LoggerFactory.getLogger(AuctionNotificationManager.class);

    // Mỗi phiên đấu giá sẽ có một "phòng thông báo" riêng biệt để quản lý danh sách các Observer đang theo dõi phiên đó
    private final Map<String, List<AuctionObserver>> roomObservers;

    // Mẫu thiết kế Bill Pugh Singleton 
    private AuctionNotificationManager() {
        this.roomObservers = new ConcurrentHashMap<>();
        // 2. Thay thế System.out bằng logger.info
        logger.info("AuctionNotificationManager (Hệ thống thông báo) đã được khởi tạo!");
    }

    private static class Helper {
        private static final AuctionNotificationManager INSTANCE = new AuctionNotificationManager();
    }

    public static AuctionNotificationManager getInstance() {
        return Helper.INSTANCE;
    }

    @Override
    // Đăng ký một người quan sát mới (Cho phép người mới bật radio dò đúng tần số).
    public void addObserver(String auctionId, AuctionObserver observer) {
        if (observer == null || auctionId == null) {
            throw new IllegalArgumentException("Observer và auctionId không được null");
        }
        roomObservers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(observer);
        
        // Cú pháp {} giúp tự động chèn biến vào chuỗi mà không cần dùng dấu + cộng chuỗi rườm rà
        logger.info("Đã đăng ký Observer mới cho Kênh {}", auctionId);
    }

    @Override
    // Hủy đăng ký một người quan sát (Người dùng tắt radio, không nghe nữa).
    public void removeObserver(String auctionId, AuctionObserver observer) {
        if (observer == null || auctionId == null) {
            throw new IllegalArgumentException("Observer và auctionId không được null");
        }
        List<AuctionObserver> room = roomObservers.get(auctionId);
        if (room != null) {
            room.remove(observer);
            logger.info("Đã hủy đăng ký Observer khỏi Kênh {}", auctionId);
            
            if (room.isEmpty()) {
                roomObservers.remove(auctionId);
                logger.debug("Kênh {} không còn ai theo dõi, đã xóa phòng thông báo.", auctionId);
            }
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH LẤY DANH SÁCH OBSERVER CHO MỘT PHIÊN ĐẤU GIÁ CỤ THỂ
    // =========================================================================
    private List<AuctionObserver> getRoomObservers(String auctionId) {
        return roomObservers.get(auctionId);
    }

    // =========================================================================
    // CÁC HÀM PHÁT THÔNG BÁO (NOTIFY) ĐẾN CLIENT
    // =========================================================================

    @Override
    public void notifyBidUpdated(String auctionId, double newBidAmount, String highestBidderId) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        if (room != null && !room.isEmpty()) {
            logger.info("[Phát thanh] Có giá mới ({}) tại Kênh {} bởi User {}", newBidAmount, auctionId, highestBidderId);
            for (AuctionObserver observer : room) {
                observer.onBidUpdated(auctionId, newBidAmount, highestBidderId);
            }
        }
    }

    @Override
    public void notifyAuctionStarted(String auctionId) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        if (room != null && !room.isEmpty()) {
            logger.info("[Phát thanh] Bắt đầu phiên tại Kênh {}", auctionId);
            for (AuctionObserver observer : room) {
                observer.onAuctionStarted(auctionId);
            }
        }
    }

    @Override
    public void notifyAuctionEnded(String auctionId) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        if (room != null && !room.isEmpty()) {
            logger.info("[Phát thanh] Hết giờ đặt giá tại Kênh {}", auctionId);
            for (AuctionObserver observer : room) {
                observer.onAuctionEnded(auctionId);
            }
        }
    }

    @Override
    public void notifyAuctionFinished(String auctionId, String winnerId, double finalPrice) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        if (room != null && !room.isEmpty()) {
            logger.info("[Phát thanh] Chốt giao dịch thành công tại Kênh {}. Người thắng: {}, Giá: {}", auctionId, winnerId, finalPrice);
            for (AuctionObserver observer : room) {
                observer.onAuctionFinished(auctionId, winnerId, finalPrice);
            }
            roomObservers.remove(auctionId);
        }
    }

    @Override
    public void notifyAuctionCancelled(String auctionId, String reason) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        if (room != null && !room.isEmpty()) {
            logger.warn("[Phát thanh] Phiên {} đã bị hủy. Lý do: {}", auctionId, reason);
            for (AuctionObserver observer : room) {
                observer.onAuctionCancelled(auctionId, reason);
            }
            roomObservers.remove(auctionId);
        }
    }
}