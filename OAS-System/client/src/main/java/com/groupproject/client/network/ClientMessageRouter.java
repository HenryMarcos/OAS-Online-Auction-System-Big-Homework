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
    private final Map<Class<? extends Response>, List<Consumer<?>>> responseListeners = new ConcurrentHashMap<>();

    // Registry for Server-pushed Pub/Sub events
    private final Map<Class<? extends ServerEvent>, List<Consumer<?>>> eventListeners = new ConcurrentHashMap<>();

    private ClientMessageRouter() {}

    // --- SUBSCRIBE TO SOLICITED RESPONSES (e.g., LoginResponse) ---
    public <T extends Response> void onResponse(Class<T> responseClass, Consumer<T> callback) {
        responseListeners.computeIfAbsent(responseClass, k -> new CopyOnWriteArrayList<>());
        responseListeners.get(responseClass).add(callback);
    }

    public <T extends Response> void offResponse(Class<T> responseClass, Consumer<T> callback) {
        List<Consumer<?>> list = responseListeners.get(responseClass);
        if (list != null) {
            list.remove(callback);
        }
    }

    // --- SUBSCRIBE TO UNSOLICITED SERVER EVENTS (e.g., NewBidEvent) ---
    public <T extends ServerEvent> void onEvent(Class<T> eventClass, Consumer<T> callback) {
        eventListeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
        eventListeners.get(eventClass).add(callback);
    }

    public <T extends ServerEvent> void offEvent(Class<T> eventClass, Consumer<T> callback) {
        List<Consumer<?>> list = eventListeners.get(eventClass);
        if (list != null) {
            list.remove(callback);
        }
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
    @SuppressWarnings("unchecked")
    private void dispatchResponse(Response response) {
        List<Consumer<?>> callbacks = responseListeners.get(response.getClass());
        if (callbacks != null && !callbacks.isEmpty()) {
            Platform.runLater(() -> {
                for (Consumer<?> callback : callbacks) {
                    ((Consumer<Response>) callback).accept(response);
                }
            });
        } else {
            ClientLogger.warning("Dropped Response. No UI component listening for: " + response.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchEvent(ServerEvent event) {
        List<Consumer<?>> callbacks = eventListeners.get(event.getClass());
        if (callbacks != null && !callbacks.isEmpty()) {
            Platform.runLater(() -> {
                for (Consumer<?> callback : callbacks) {
                    ((Consumer<ServerEvent>) callback).accept(event);
                }
            });
        } else {
            ClientLogger.warning("Dropped ServerEvent. No UI component listening for: " + event.getClass().getSimpleName());
        }
    }
    
}
