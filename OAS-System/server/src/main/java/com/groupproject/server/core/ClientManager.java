package com.groupproject.server.core;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.events.ServerEvent;

public enum ClientManager {
    INSTANCE;

    // Danh sách chứa tất cả client
    private final List<ObjectOutputStream> clients = new ArrayList<>();

    // Bản đồ các phòng đấu giá (Key: Auction ID, Value: Tập hợp các clients trong phòng đó)
    private final Map<Integer, Set<ObjectOutputStream>> auctionRooms = new ConcurrentHashMap<>();

    private ClientManager() {}

    // --- QUẢN LÝ CLIENT CHUNG ---
    public void addClient(ObjectOutputStream out) {
        synchronized (clients) {
            clients.add(out);
        }
    }

    public void removeClient(ObjectOutputStream out) {
        synchronized (clients) {
            clients.remove(out);
        }
    }

    public List<ObjectOutputStream> getClients() { return clients; }

    // --- QUẢN LÝ PHÒNG ĐẤU GIÁ ---
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

    // --- HÀM GỬI EVENT TỚI 1 PHÒNG CỤ THỂ ---
    public void broadcastEventToAuction(int auctionId, ServerEvent event) {
        Set<ObjectOutputStream> roomClients = auctionRooms.get(auctionId);
        
        if (roomClients != null) {
            for (ObjectOutputStream writer : roomClients) {
                sendEventSafely(writer, event);
            }
        }
    }

    // --- HÀM GỬI EVENT TỚI TOÀN BỘ HỆ THỐNG ---
    public void broadcastSystemEvent(ServerEvent event) {
        synchronized (clients) {
            for (ObjectOutputStream writer : clients) {
                sendEventSafely(writer, event);
            }
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
}
