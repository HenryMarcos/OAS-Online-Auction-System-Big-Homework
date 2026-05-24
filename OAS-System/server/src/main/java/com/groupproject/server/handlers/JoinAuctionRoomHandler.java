package com.groupproject.server.handlers;

import java.io.ObjectOutputStream;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.request.JoinAuctionRoomRequest;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.JoinAuctionRoomResponse;
import com.groupproject.shared.network.response.Response;

public class JoinAuctionRoomHandler implements RequestHandler {
    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        if (!(request instanceof JoinAuctionRoomRequest)) {
            return new JoinAuctionRoomResponse(false, "Invalid request format");
        }

        JoinAuctionRoomRequest joinReq = (JoinAuctionRoomRequest) request;
        int auctionId = joinReq.getAuctionId();

        // Móc 'out' từ ClientContext ra (đã được lưu lúc client kết nối)
        ObjectOutputStream out = ClientContext.currentOut.get();

        if (out != null) {
            ClientManager.getInstance().subscribeToAuction(auctionId, out);
            ServerLogger.info("Client successfully joined auction room: " + auctionId);
            return new JoinAuctionRoomResponse(true, "Joined room " + auctionId + " successfully.");
        } else {
            ServerLogger.error("Could not find Output Stream for Client in Context.");
            return new JoinAuctionRoomResponse(false, "Internal Server Error: No Output Stream found.");
        }
    }
}