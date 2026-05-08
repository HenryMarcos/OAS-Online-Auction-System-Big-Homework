package com.groupproject.server.handlers;

import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public interface RequestHandler {
    Response handle(Request request);
}
