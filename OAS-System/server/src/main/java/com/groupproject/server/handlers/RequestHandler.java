package com.groupproject.server.handlers;

import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.Response;

public interface RequestHandler {
    Response handle(Request request);
}
