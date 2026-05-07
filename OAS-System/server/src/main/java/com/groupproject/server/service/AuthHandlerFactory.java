package com.groupproject.server.service;

public class AuthHandlerFactory {
    public static AuthHandler getHandler(String type) {
        if ("LOGIN".equals(type)) {
            return new LoginHandler();
        } else if ("SIGNUP".equals(type)) {
            return new SignupHandler();
        } 
        throw new IllegalArgumentException("Unkown auth type: " + type);
    }
}
