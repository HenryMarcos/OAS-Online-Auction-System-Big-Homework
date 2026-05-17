package com.groupproject.shared.network;
import com.groupproject.shared.model.enums.*;
public class GetAuctionItemRequest extends Request {
    public enum FilterType {
        ALL, // Lấy tất cả các phiên đấu giá 
        BY_SELLER,  // Lấy tất cả những phiên đấu giá của tôi;
        BY_STATUS  // Lấy tất cả những phiên đấu giá theo trạng thái ;
    }
    private FilterType filterType;
    private int sellerId;
    private AuctionStatus status;
    // ĐẶT CÁC PHƯƠNG THỨC LÀ STATIC 

    // GOI HAM LAY TAT CA CAC PHIEN DAU GIA 
    public static GetAuctionItemRequest getAllAuctionItems() {
        GetAuctionItemRequest request = new GetAuctionItemRequest();
        request.filterType= FilterType.ALL;
        return request;
    }
    // HÀM GỌI LẤY TẤT CẢ CÁC PHIÊN ĐẤU GIÁ CỦA TÔI
    public static GetAuctionItemRequest getMyAuctionItems(int sellerId) {
        GetAuctionItemRequest request = new GetAuctionItemRequest();
        request.filterType= FilterType.BY_SELLER;
        request.sellerId = sellerId;
        return  request;
    }
    // HÀM GỌI LẤY TẤT CẢ CÁC PHIÊN ĐẤU GIÁ CÓ TRẠNG THÁI LÀ ACTIVED
    public static GetAuctionItemRequest getActivedAuctionItems() {
        GetAuctionItemRequest request = new GetAuctionItemRequest();
        request.filterType= FilterType.BY_STATUS;
        request.status= AuctionStatus.ACTIVED;
        return request;
    }
    
}
    
