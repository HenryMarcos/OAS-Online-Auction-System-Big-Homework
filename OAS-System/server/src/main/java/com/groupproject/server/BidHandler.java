package com.groupproject.server;

import java.io.ObjectOutputStream;

import com.groupproject.shared.AuctionUpdate;
import com.groupproject.shared.BidRequest;

public class BidHandler {
    public void handle(BidRequest bidRequest, ObjectOutputStream senderOut) throws Exception {
        // Kiểm tra database sql xem bid hiện tại có cao hơn bid trước không
        boolean isValidBid = AuctionManager.proccessBid(bidRequest);

        if (isValidBid) {
            // Tạo update
            AuctionUpdate update = new AuctionUpdate(bidRequest);

            // Thông báo cho các user
            synchronized (ServerApp.clientWriters) {
                for (ObjectOutputStream clientOut: ServerApp.clientWriters) {
                    clientOut.writeObject(update);
                    clientOut.flush();
                }
            }
        } else {
            // Thông báo user rằng bid của họ quá thấp
            senderOut.writeObject("SERVER:BID_FAIL:Bid too low!");
            senderOut.flush();
        }
    }
}
