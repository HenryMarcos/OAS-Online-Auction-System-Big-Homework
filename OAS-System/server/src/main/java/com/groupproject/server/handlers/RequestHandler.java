package com.groupproject.server.handlers;

import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.Response;

public interface RequestHandler {
    Response handle(Request request);
}
