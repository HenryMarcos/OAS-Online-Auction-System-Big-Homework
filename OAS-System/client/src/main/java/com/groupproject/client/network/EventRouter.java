package com.groupproject.client.network;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.groupproject.shared.network.Response;

import javafx.application.Platform;

public class EventRouter {

    private static EventRouter instance;

    
    private final Map< Class<? extends Response>, List<Consumer<Response>> > listeners;

    private EventRouter() {
        // ConcurrentHashmap đảm bảo an toàn đa luồng
        this.listeners = new ConcurrentHashMap<>();
    }

    // Singleton
    public static EventRouter getInstance() {
        if (instance == null) {
            instance = new EventRouter();
        }
        return instance;
    }
    
    // Thêm người nghe
    // Gắn 1 hàm controller cho 1 Lớp response nhất định từ server
    @SuppressWarnings("unchecked")
    public <T extends Response> void on(Class<T> responseClass, Consumer<T> callback) {

        // Nếu đây là lần đầu tiên nghe event này thì tạo 1 list mới (an toàn đa luồng)
        listeners.computeIfAbsent(responseClass, k -> new CopyOnWriteArrayList<>());

        // Thêm listener vào list
        listeners.get(responseClass).add(res -> callback.accept((T) res));
    }

    // Thông báo Response sắp tới cho tất cả những controller đã đăng ký Response cụ thể ấy
    public void dispatch(Response response) {

       
        List<Consumer<Response>> callbacks = listeners.get(response.getClass());

        if (callbacks != null && !callbacks.isEmpty()) {
            Platform.runLater(() -> {
                // Chạy qua tất cả controller đang nghe response này và kích hoạt hàm của chúng
                for (Consumer<Response> callback : callbacks) {
                    callback.accept(response);
                }
            });
        } else {
            System.out.println("Warning: Dropped Response. Nobody is listening for -> " + response.getClass().getSimpleName());
        }
    }

    // Tùy chọn: Nếu muốn xóa bộ nhớ khi mà user log out
    public void clearAllListeners() {
        listeners.clear();
    }
}
