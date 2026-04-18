package com.groupproject.server.Authentication;

import java.io.ObjectOutputStream;

import com.groupproject.shared.AuthRequest;

public interface AuthHandler {
    void handle(AuthRequest request, ObjectOutputStream out) throws Exception;
}
