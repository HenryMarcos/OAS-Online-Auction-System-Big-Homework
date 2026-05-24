package com.groupproject.server.handlers;

import java.util.List;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.GetMyAuctionsRequest;
import com.groupproject.shared.network.GetMyAuctionsResponse;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public class GetMyAuctionHandler implements RequestHandler {

    @Override
    public Response handle(Request request) {
        // Ép kiểu request chung thành request cụ thể
        GetMyAuctionsRequest req = (GetMyAuctionsRequest) request;
        
        // Lấy danh sách từ DAO
        List<Auction> list = AuctionDAO.getAuctionsBySeller(req.getSellerId());
        
        // Trả về Response, ClientHandler sẽ tự động lấy cái này và send qua out.writeObject()
        return new GetMyAuctionsResponse(true, "Lấy danh sách thành công", list);
    }
}