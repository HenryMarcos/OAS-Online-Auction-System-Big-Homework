package com.groupproject.server.handlers;

import java.util.HashMap;
import java.util.Map;

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
    }

    public Response dispatch(Request request) {
        RequestHandler handler = handlers.get(request.getClass());

        if (handler != null) {
            return handler.handle(request);
        } else {
            System.err.println("No handler found for: " + request.getClass().getSimpleName());
            return null; // Or return a generic ErrorResponse
        }
        
    }
}
