package com.groupproject.client.network;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.groupproject.client.utils.ClientLogger;
import com.groupproject.shared.network.events.ServerEvent;
import com.groupproject.shared.network.responses.Response;
import javafx.application.Platform;

public enum ClientMessageRouter {
    INSTANCE;

    // Registry for Request-Response patterns
    private final Map<Class<? extends Response>, List<Consumer<Response>>> responseListeners = new ConcurrentHashMap<>();

    // Registry for Server-pushed Pub/Sub events
    private final Map<Class<? extends ServerEvent>, List<Consumer<ServerEvent>>> eventListeners = new ConcurrentHashMap<>();

    private ClientMessageRouter() {}

    // --- SUBSCRIBE TO SOLICITED RESPONSES (e.g., LoginResponse) ---
    @SuppressWarnings("unchecked")
    public <T extends Response> void onResponse(Class<T> responseClass, Consumer<T> callback) {
        responseListeners.computeIfAbsent(responseClass, k -> new CopyOnWriteArrayList<>());
        responseListeners.get(responseClass).add(res -> callback.accept((T) res));
    }

    // --- SUBSCRIBE TO UNSOLICITED SERVER EVENTS (e.g., NewBidEvent) ---
    @SuppressWarnings("unchecked")
    public <T extends ServerEvent> void onEvent(Class<T> eventClass, Consumer<T> callback) {
        eventListeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
        eventListeners.get(eventClass).add(ev -> callback.accept((T) ev));
    }

    // --- UNSUBSCRIBE MECHANISMS (To prevent memory leaks when screens close) ---
    public void clearAllListeners() {
        responseListeners.clear();
        eventListeners.clear();
        ClientLogger.info("All network listeners cleared.");
    }

    // --- THE ONE CENTRAL ENTRY POINT FOR YOUR BACKGROUND NETWORK THREAD ---
    public void handleIncomingMessage(Object message) {
        if (message instanceof Response response) {
            dispatchResponse(response);
        } else if (message instanceof ServerEvent event) {
            dispatchEvent(event);
        } else {
            ClientLogger.warning("Unknown object type received over network: " + message.getClass().getSimpleName());
        }
    }

    // --- PRIVATE ROUTING HELPER METHODS ---
    private void dispatchResponse(Response response) {
        List<Consumer<Response>> callbacks = responseListeners.get(response.getClass());
        if (callbacks != null && !callbacks.isEmpty()) {
            Platform.runLater(() -> {
                for (Consumer<Response> callback : callbacks) {
                    callback.accept(response);
                }
            });
        } else {
            ClientLogger.warning("Dropped Response. No UI component listening for: " + response.getClass().getSimpleName());
        }
    }

    private void dispatchEvent(ServerEvent event) {
        List<Consumer<ServerEvent>> callbacks = eventListeners.get(event.getClass());
        if (callbacks != null && !callbacks.isEmpty()) { // Fixed the .isEmpty() logic bug here!
            Platform.runLater(() -> {
                for (Consumer<ServerEvent> callback : callbacks) {
                    callback.accept(event);
                }
            });
        } else {
            ClientLogger.warning("Dropped ServerEvent. No UI component listening for: " + event.getClass().getSimpleName());
        }
    }
    
}
