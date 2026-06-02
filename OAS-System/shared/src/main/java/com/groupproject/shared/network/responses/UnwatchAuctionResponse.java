package com.groupproject.shared.network.responses;

public class UnwatchAuctionResponse extends Response {
    private static final long serialVersionUID = 1L;

    public UnwatchAuctionResponse(boolean success, String message) {
        super(success, message);
    }
}
