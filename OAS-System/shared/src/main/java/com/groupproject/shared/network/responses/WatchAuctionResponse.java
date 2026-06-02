package com.groupproject.shared.network.responses;

public class WatchAuctionResponse extends Response {
    private static final long serialVersionUID = 1L;

    public WatchAuctionResponse(boolean success, String message) {
        super(success, message);
    }
}
