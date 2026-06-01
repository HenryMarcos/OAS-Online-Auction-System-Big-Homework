package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.requests.LogOutRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.LogOutResponse;
import com.groupproject.shared.network.responses.Response;

public class LogOutHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        if (!(request instanceof LogOutRequest)) {
            ServerLogger.warning("This request is not LoginRequest but " + request.getClass().getSimpleName());
            return new LogOutResponse(false, "Failed to log out");
        }

        clientContext.setAuthenticatedUserId(null);

        return new LogOutResponse(true, "Successfully log out");
    }
}
