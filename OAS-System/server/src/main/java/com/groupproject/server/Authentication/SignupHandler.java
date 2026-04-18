package com.groupproject.server.Authentication;

import java.io.ObjectOutputStream;

import com.groupproject.shared.AuthRequest;

public class SignupHandler implements AuthHandler {
    @Override
    public void handle(AuthRequest request, ObjectOutputStream out) throws Exception {
        boolean success = DatabaseHelper.saveUser(request);

        out.writeObject(success ? "SERVER:AUTH_SUCCESS" : "SERVER:AUTH_FAIL:User exists!");
        out.flush();
    }
}
