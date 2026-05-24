package com.groupproject.server.handlers;

import java.util.List;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.utils.ClientContext; 
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.GetMyAuctionsResponse;
import com.groupproject.shared.network.response.Response;

public class GetMyAuctionHandler implements RequestHandler {

    @Override
    public Response handle(Request request) {
        // Lấy ID người dùng trực tiếp từ Session/Context của Server (Bảo mật hơn)
        int currentUserId = ClientContext.currentUser.get().getId();
        
        // Lấy danh sách đấu giá mà người này là chủ (seller)
        List<Auction> list = AuctionDAO.getAuctionsBySeller(currentUserId);
        
        return new GetMyAuctionsResponse(true, "Lấy danh sách thành công", list);
    }
}