package com.groupproject.server.core;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.event.ServerEvent;

public class ClientManager {
    private static ClientManager instance;

    // Danh sách chứa TẤT CẢ client (Luồng 1)
    private final List<ObjectOutputStream> clients = new ArrayList<>();

    // Danh sách chứa CHỈ CÁC ADMIN (Luồng 3 - MỚI)
    private final Set<ObjectOutputStream> adminClients = ConcurrentHashMap.newKeySet();

    // Bản đồ các phòng đấu giá (Luồng 2 - Key: Auction ID, Value: Tập hợp các clients trong phòng đó)
    private final Map<Integer, Set<ObjectOutputStream>> auctionRooms = new ConcurrentHashMap<>();

    private ClientManager() {}

    public static synchronized ClientManager getInstance() {
        if (instance == null) { instance = new ClientManager(); }
        return instance;
    }

    // --- 1. QUẢN LÝ CLIENT CHUNG & ADMIN ---
    
    public void addClient(ObjectOutputStream out) {
        synchronized (clients) {
            clients.add(out);
        }
    }

    // MỚI: Đăng ký một luồng kết nối là Admin
    public void addAdminClient(ObjectOutputStream out) {
        adminClients.add(out);
        ServerLogger.info("An Admin client has been registered for admin-only broadcasts.");
    }

    public void removeClient(ObjectOutputStream out) {
        synchronized (clients) {
            clients.remove(out);
        }
        adminClients.remove(out); // Xóa khỏi danh sách Admin nếu có
    }

    public List<ObjectOutputStream> getClients() { return clients; }

    // --- 2. QUẢN LÝ PHÒNG ĐẤU GIÁ ---
    
    public void subscribeToAuction(int auctionId, ObjectOutputStream clientOut) {
        auctionRooms.putIfAbsent(auctionId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(auctionId).add(clientOut);
        ServerLogger.info("A client joined auction room: " + auctionId);
    }

    public void unsubscribeToAuction(int auctionId, ObjectOutputStream clientOut) {
        if (auctionRooms.containsKey(auctionId)) {
            auctionRooms.get(auctionId).remove(clientOut);
            if (auctionRooms.get(auctionId).isEmpty()) {
                auctionRooms.remove(auctionId); // Dọn dẹp phòng trống
            }
        }
    }

    public void removeAuctionRoom(int auctionId) {
        if (auctionRooms.containsKey(auctionId)) {
            auctionRooms.remove(auctionId);
            ServerLogger.info("Auction Room " + auctionId + " has been removed from ClientManager.");
        }
    }

    // --- 3. CÁC HÀM BROADCAST (3 LUỒNG) ---

    // LUỒNG 2: GỬI EVENT TỚI 1 PHÒNG CỤ THỂ (Dành cho việc đặt giá, đếm ngược)
    public void broadcastEventToAuction(int auctionId, ServerEvent event) {
        Set<ObjectOutputStream> roomClients = auctionRooms.get(auctionId);
        if (roomClients != null) {
            for (ObjectOutputStream writer : roomClients) {
                sendEventSafely(writer, event);
            }
        }
    }

    // LUỒNG 1: GỬI EVENT TỚI TOÀN BỘ HỆ THỐNG (Dành cho cập nhật UI trang chủ)
    public void broadcastSystemEvent(ServerEvent event) {
        synchronized (clients) {
            for (ObjectOutputStream writer : clients) {
                sendEventSafely(writer, event);
            }
        }
    }

    // LUỒNG 3: GỬI EVENT CHỈ CHO ADMIN (MỚI - Dành cho Log hệ thống)
    public void broadcastToAdmins(ServerEvent event) {
        for (ObjectOutputStream writer : adminClients) {
            sendEventSafely(writer, event);
        }
    }

    // Hàm helper để gửi an toàn và chống lặp code
    private void sendEventSafely(ObjectOutputStream writer, ServerEvent event) {
        try {
            writer.writeObject(event);
            writer.flush();
            writer.reset(); // Bắt buộc để tránh gửi object cũ trong cache
        } catch (Exception e) {
            ServerLogger.error("Failed to send ServerEvent: " + e.getMessage());
        }
    }

    // --- 4. HÀM DỌN DẸP ---
    
    // Hàm dọn dẹp toàn bộ tài nguyên liên quan đến client khi họ rời đi
    public void removeClientCompletely(ObjectOutputStream out) {
        // 1. Xóa khỏi danh sách chung (và danh sách Admin)
        removeClient(out);

        // 2. Càn quét toàn bộ các phòng và đuổi client này ra
        for (Map.Entry<Integer, Set<ObjectOutputStream>> entry : auctionRooms.entrySet()) {
            Set<ObjectOutputStream> roomClients = entry.getValue();
            if (roomClients.contains(out)) {
                roomClients.remove(out);
                
                // Nếu phòng trống thì dẹp phòng luôn
                if (roomClients.isEmpty()) {
                    auctionRooms.remove(entry.getKey());
                }
            }
        }
        ServerLogger.info("Cleaned up all resources for disconnected client.");
    }
}