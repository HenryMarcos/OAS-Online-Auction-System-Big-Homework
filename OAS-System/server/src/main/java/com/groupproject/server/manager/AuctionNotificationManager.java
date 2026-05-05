package com.groupproject.server.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.groupproject.server.pattern.observer.AuctionObserver;
import com.groupproject.server.pattern.observer.AuctionSubject;

public class AuctionNotificationManager implements AuctionSubject {
    
    // Mỗi phiên đấu giá sẽ có một "phòng thông báo" riêng biệt để quản lý danh sách các Observer đang theo dõi phiên đó
    private final Map<String, List<AuctionObserver>> roomObservers;

    // Mẫu thiết kế Bill Pugh Singleton 
    private AuctionNotificationManager() {
        this.roomObservers = new ConcurrentHashMap<>();
        System.out.println("AuctionNotificationManager (Hệ thống thông báo) đã được khởi tạo!");
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
        // Kiểm tra đầu vào, đảm bảo không null và có ID hợp lệ
        if (observer == null || auctionId == null) {
            throw new IllegalArgumentException("Observer và auctionId không được null");
        }
        // Sử dụng computeIfAbsent để tạo mới danh sách Observer cho auctionId nếu chưa tồn tại, sau đó thêm observer vào danh sách
        roomObservers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(observer);
        System.out.println("Đã đăng ký Observer mới cho Kênh " + auctionId);
    }

    @Override
    // Hủy đăng ký một người quan sát (Người dùng tắt radio, không nghe nữa).
    public void removeObserver(String auctionId, AuctionObserver observer) {
        // Kiểm tra đầu vào, đảm bảo không null và có ID hợp lệ
        if (observer == null || auctionId == null) {
            throw new IllegalArgumentException("Observer và auctionId không được null");
        }
        List<AuctionObserver> room = roomObservers.get(auctionId);
        if (room != null) {
            room.remove(observer);
            System.out.println("Đã hủy đăng ký Observer khỏi Kênh " + auctionId);
            if (room.isEmpty()) {
                // Nếu không còn ai theo dõi phiên đấu giá này nữa, có thể xóa luôn phòng thông báo để giải phóng tài nguyên
                roomObservers.remove(auctionId);
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
    // Thông báo có giá mới được đặt thành công.
    public void notifyBidUpdated(String auctionId, double newBidAmount, String highestBidderId) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        // Nếu có ai đang theo dõi phiên đấu giá này thì mới phát thông báo
        if (room != null && !room.isEmpty()) {
            System.out.println("[Phát thanh] Có giá mới (" + newBidAmount + ") tại Kênh " + auctionId);
            // Phát thông báo đến tất cả những ai đang theo dõi phiên đấu giá này
            for (AuctionObserver observer : room) {
                // Gọi hàm onBidUpdated() của mỗi Observer để họ cập nhật giao diện hoặc trạng thái bên client
                observer.onBidUpdated(auctionId, newBidAmount, highestBidderId);
            }
        }
    }

    @Override
    // Thông báo phiên đấu giá đã bắt đầu.
    public void notifyAuctionStarted(String auctionId) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        // Nếu có ai đang theo dõi phiên đấu giá này thì mới phát thông báo
        if (room != null && !room.isEmpty()) {
            System.out.println("[Phát thanh] Bắt đầu phiên tại Kênh " + auctionId);
            // Phát thông báo đến tất cả những ai đang theo dõi phiên đấu giá này
            for (AuctionObserver observer : room) {
                // Gọi hàm onAuctionStarted() của mỗi Observer để họ cập nhật giao diện hoặc trạng thái bên client
                observer.onAuctionStarted(auctionId);
            }
        }
    }

    @Override
    // Thông báo phiên đấu giá đã hết giờ.
    public void notifyAuctionEnded(String auctionId) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        // Nếu có ai đang theo dõi phiên đấu giá này thì mới phát thông báo
        if (room != null && !room.isEmpty()) {
            System.out.println("[Phát thanh] Hết giờ đặt giá tại Kênh " + auctionId);
            // Phát thông báo đến tất cả những ai đang theo dõi phiên đấu giá này
            for (AuctionObserver observer : room) {
                // Gọi hàm onAuctionEnded() của mỗi Observer để họ cập nhật giao diện hoặc trạng thái bên client
                observer.onAuctionEnded(auctionId);
            }
        }
    }

    @Override
    // Thông báo phiên đấu giá đã hoàn tất và chốt giao dịch.
    public void notifyAuctionFinished(String auctionId, String winnerId, double finalPrice) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        // Nếu có ai đang theo dõi phiên đấu giá này thì mới phát thông báo
        if (room != null && !room.isEmpty()) {
            System.out.println("[Phát thanh] Chốt giao dịch thành công tại Kênh " + auctionId);
            // Phát thông báo đến tất cả những ai đang theo dõi phiên đấu giá này
            for (AuctionObserver observer : room) {
                // Gọi hàm onAuctionFinished() của mỗi Observer để họ cập nhật giao diện hoặc trạng thái bên client
                observer.onAuctionFinished(auctionId, winnerId, finalPrice);
            }
            
            // Xóa toàn bộ phòng thông báo trên RAM sau khi đã hoàn tất giao dịch
            roomObservers.remove(auctionId);
        }
    }

    @Override
    // Thông báo phiên đấu giá đã bị hủy bỏ giữa chừng.
    public void notifyAuctionCancelled(String auctionId, String reason) {
        List<AuctionObserver> room = getRoomObservers(auctionId);
        // Nếu có ai đang theo dõi phiên đấu giá này thì mới phát thông báo
        if (room != null && !room.isEmpty()) {
            System.out.println("[Phát thanh] Phiên " + auctionId + " đã bị hủy. Lý do: " + reason);
            // Phát thông báo đến tất cả những ai đang theo dõi phiên đấu giá này
            for (AuctionObserver observer : room) {
                // Gọi hàm onAuctionCancelled() của mỗi Observer để họ cập nhật giao diện hoặc trạng thái bên client
                observer.onAuctionCancelled(auctionId, reason);
            }
            
            // Xóa toàn bộ phòng thông báo trên RAM sau khi đã hủy phiên đấu giá
            roomObservers.remove(auctionId);
        }
    }
}