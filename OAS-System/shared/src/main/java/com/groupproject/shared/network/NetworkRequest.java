package com.groupproject.shared.network;

public class NetworkRequest extends Request {
    private String action;
    private Object payload;

    public NetworkRequest(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() { return action; }
    public Object getPayload() { return payload; }
}
