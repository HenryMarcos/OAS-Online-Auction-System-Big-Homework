package com.groupproject.server.handlers;

import java.util.HashMap;
import java.util.Map;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.requests.GetAuctionRequest;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.requests.LogOutRequest;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.PlaceBidRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.requests.SignupRequest;
import com.groupproject.shared.network.requests.TopUpRequest;
import com.groupproject.shared.network.requests.UnwatchAuctionRequest;
import com.groupproject.shared.network.requests.WatchAuctionRequest;
import com.groupproject.shared.network.responses.Response;

public class RequestDispatcher {
    private final Map<Class<? extends Request>, RequestHandler> handlers = new HashMap<>();

    public RequestDispatcher() {
        // Nối các request với handler
        handlers.put(LoginRequest.class, new LoginHandler());
        handlers.put(SignupRequest.class, new SignupHandler());
        handlers.put(LogOutRequest.class, new LogOutHandler());
        handlers.put(CreateAuctionRequest.class, new CreateAuctionHandler());
        handlers.put(PlaceBidRequest.class, new PlaceBidHandler());
        handlers.put(JoinAuctionRequest.class, new JoinAuctionHandler());
        handlers.put(com.groupproject.shared.network.requests.ChangeAuctionStatusRequest.class, new ChangeAuctionStatusHandler());
        handlers.put(TopUpRequest.class, new TopUpHandler());
        handlers.put(WatchAuctionRequest.class, new WatchAuctionHandler());
        handlers.put(UnwatchAuctionRequest.class, new UnwatchAuctionHandler());
        handlers.put(GetAuctionRequest.class, new GetAuctionHandler());
    }

    public Response dispatch(Request request, ClientHandler clientContext) {
        ServerLogger.info("Getting suitable Handler for " + request.getClass().getSimpleName());
        RequestHandler handler = handlers.get(request.getClass());

        if (handler != null) {
            ServerLogger.info("Got suitable Handler: " + handler.getClass().getSimpleName());
            return handler.handle(request, clientContext);
        } else {
            ServerLogger.error("No handler found for: " + request.getClass().getSimpleName());
            return null; // Or return a generic ErrorResponse
        }
        
    }
}
