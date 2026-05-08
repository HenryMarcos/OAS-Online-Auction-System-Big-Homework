package com.groupproject.shared.network;

import java.io.Serializable;

public abstract class Response implements Serializable{

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public Response(boolean success) {
        this(success, null);
    }

    public abstract String getType();

    public boolean isSuccess() { return success; }
    public String getMessage() {return message; }
}
