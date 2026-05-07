package com.groupproject.shared;

import java.io.Serializable;

public class NetworkRequest implements Serializable {
    private String action;
    private Object payload;

    public NetworkRequest(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() { return action; }
}
