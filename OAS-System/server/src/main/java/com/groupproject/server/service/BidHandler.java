package com.groupproject.server.service;

import java.io.ObjectOutputStream;

import com.groupproject.server.core.ClientManager;
import com.groupproject.shared.AuctionUpdate;
import com.groupproject.shared.network.BidRequest;

public class BidHandler {
    public void handle(BidRequest bidRequest, ObjectOutputStream senderOut) throws Exception {
        // Kiểm tra database sql xem bid hiện tại có cao hơn bid trước không
        boolean isValidBid = AuctionManager.proccessBid(bidRequest);

        if (isValidBid) {
            // Tạo update
            AuctionUpdate update = new AuctionUpdate(bidRequest);

            // Thông báo cho các user
            synchronized (ClientManager.getInstance().getClients()) {
                for (ObjectOutputStream clientOut: ClientManager.getInstance().getClients()) {
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
