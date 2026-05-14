package com.groupproject.server.handlers;

import java.util.HashMap;
import java.util.Map;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.CreateAuctionRequest;
import com.groupproject.shared.network.LoginRequest;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;
import com.groupproject.shared.network.SignupRequest;

public class RequestDispatcher {
    private final Map<Class<? extends Request>, RequestHandler> handlers = new HashMap<>();

    public RequestDispatcher() {
        // Nối các request với handler
        handlers.put(LoginRequest.class, new LoginHandler());
        handlers.put(SignupRequest.class, new SignupHandler());
        handlers.put(CreateAuctionRequest.class, new CreateAuctionHandler());
    }

    public Response dispatch(Request request) {
        ServerLogger.info("Getting suitable Handler for " + request.getClass().getSimpleName());
        RequestHandler handler = handlers.get(request.getClass());

        if (handler != null) {
            ServerLogger.info("Got suitable Handler: " + handler.getClass().getSimpleName());
            return handler.handle(request);
        } else {
            ServerLogger.error("No handler found for: " + request.getClass().getSimpleName());
            return null; // Or return a generic ErrorResponse
        }
        
    }
}
