package com.groupproject.client.network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.groupproject.shared.network.AuctionEvent.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import javafx.application.Platform;
// NƠI ĐĂNG KÝ NHỮNG NGƯỜI MUỐN NHẬN THÔNG BÁO TỪ PHIÊN ĐẤU GIÁ VÀ PHÁT THÔNG BÁO 
public class AuctionEventBus {
    private static final Logger logger = LoggerFactory.getLogger(AuctionEventBus.class);
    // TAO RA MOT PHONG CHO DE GUI DI NHUNG THONG BAO CUA PHONG CHO DO DEN CHO NHUNG NGUOI THEO DOI
    private final Map<Integer,List<AuctionListener>> rooms = new ConcurrentHashMap<>();
    private AuctionEventBus() {}
    private static class Helper{
        private static final AuctionEventBus INSTANCE = new AuctionEventBus();
    }
    public static AuctionEventBus getInstance() {
        return Helper.INSTANCE;
    }
     // ── Đăng ký theo dõi một phiên đấu giá ──
    public void subscribe(int auctionId, AuctionListener listener) {
        if ((Integer) auctionId == null || listener == null)
            throw new IllegalArgumentException("auctionId và listener không được null");

        rooms.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.info("[EventBus] Listener đăng ký theo dõi phiên {}", auctionId);
    }

    // ── Hủy theo dõi (khi rời màn hình) ──
    public void unsubscribe(int auctionId, AuctionListener  listener) {
        rooms.compute(auctionId, (id, room) -> {
            if (room == null) return null;
            room.remove(listener);
            logger.info("[EventBus] Listener rời phiên {}", id);
            return room.isEmpty() ? null : room;
        });
    }

    // ── Phân phát event tới đúng phòng ──
    // OCP: thêm event mới không cần sửa hàm này
    public void publish(AuctionEvent event) {
        List<AuctionListener> room = rooms.get(event.getAuctionId());
        if (room == null || room.isEmpty()) return;

        logger.info("[EventBus] Phát event {} tới {} listener của phiên {}",
            event.getClass().getSimpleName(), room.size(), event.getAuctionId());

        // Dispatch đúng method theo kiểu event — không dùng instanceof chain
        Platform.runLater(() -> {
            for (AuctionListener listener : room) {
                dispatch(event, listener);
            }
        });
    }

    // DRY: tập trung logic dispatch tại một chỗ duy nhất
    private void dispatch(AuctionEvent event, AuctionListener listener) {
        try {
            event.accept(listener);
        }
        catch (Exception e) {
            logger.error("[EventBus] Lỗi khi dispatch event tới listener : {}", e.getMessage());
        }
    }
}
