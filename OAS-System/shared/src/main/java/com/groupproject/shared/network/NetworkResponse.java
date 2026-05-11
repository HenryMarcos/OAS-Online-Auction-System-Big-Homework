package com.groupproject.shared.network;

public class NetworkResponse extends Response {
    private String action;
    public Object payload;

    public NetworkResponse(String action, boolean isSuccess, Object payload, String message) {
        super(isSuccess, message);
        this.action = action;
        this.payload = payload;
    }

    public NetworkResponse(String action, boolean isSuccess, String message) {
        this(action, isSuccess, null, message);
    }

    public NetworkResponse(String action, boolean isSuccess) {
        this(action, isSuccess, null, null);
    }

    public String getAction() { return action; }
    public Object getPayload() { return payload; }
}
