package com.groupproject.shared.network.AuctionEvent;

public interface AuctionListener {
    // CÓ GIÁ MỚI CẬP NHẬT -> CẬP NHẬT LẠI UI
    default void onBidUpdated(BidUpdatedEvent event){}
    // BẮT ĐẦU PHIÊN ĐẤU GIÁ - MỞ KHÓA NÚT ĐẶT GIÁ (HOẶC CÓ THỂ NÚT VÀO PHÒNG ĐẤU GIÁ)
    default void onAuctionStarted (AuctionStartedEvent event){}
    // BẮT ĐẦU VÔ HIỆU HÓA NÚT ĐẶT GIÁ 
    default void onAuctionEnded(AuctionEndedEvent event){}
    // KẾT THÚC 
    default void onAuctionFinished(AuctionFinisedEvent event){}
    // HỦY PHIÊN ĐẤU GIÁ -> TỰ ĐỘNG RA MÀN HÌNH CHÍNH 
    default void onAuctionCancelled(AuctionCancelledEvent event){}
}
