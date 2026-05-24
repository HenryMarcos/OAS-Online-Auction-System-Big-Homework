package com.groupproject.server.handlers;

import java.io.ObjectOutputStream;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.request.LeaveAuctionRoomRequest;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.LeaveAuctionRoomResponse;
import com.groupproject.shared.network.response.Response;

public class LeaveAuctionRoomHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        if (!(request instanceof LeaveAuctionRoomRequest)) {
            return new LeaveAuctionRoomResponse(false, "Invalid request format");
        }

        LeaveAuctionRoomRequest leaveReq = (LeaveAuctionRoomRequest) request;
        int auctionId = leaveReq.getAuctionId();

        // Móc 'out' từ ClientContext ra
        ObjectOutputStream out = ClientContext.currentOut.get();

        if (out != null) {
            ClientManager.getInstance().unsubscribeToAuction(auctionId, out);
            ServerLogger.info("Client left auction room: " + auctionId);
            return new LeaveAuctionRoomResponse(true, "Left room " + auctionId + " successfully.");
        } else {
            ServerLogger.error("Could not find Output Stream for Client in Context.");
            return new LeaveAuctionRoomResponse(false, "Internal Server Error: No Output Stream found.");
        }
    }
}