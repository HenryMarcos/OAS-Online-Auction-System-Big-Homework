package com.groupproject.client.network;

import com.groupproject.shared.network.events.AuctionCancelledEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.events.AuctionFinisedEvent;
import com.groupproject.shared.network.events.AuctionStartedEvent;
import com.groupproject.shared.network.events.NewBidEvent;

public interface AuctionListener {
    // CÓ GIÁ MỚI CẬP NHẬT -> CẬP NHẬT LẠI UI
    default void onBidUpdated(NewBidEvent event){}
    // BẮT ĐẦU PHIÊN ĐẤU GIÁ - MỞ KHÓA NÚT ĐẶT GIÁ (HOẶC CÓ THỂ NÚT VÀO PHÒNG ĐẤU GIÁ)
    default void onAuctionStarted (AuctionStartedEvent event){}
    // BẮT ĐẦU VÔ HIỆU HÓA NÚT ĐẶT GIÁ 
    default void onAuctionEnded(AuctionEndedEvent event){}
    // KẾT THÚC 
    default void onAuctionFinished(AuctionFinisedEvent event){}
    // HỦY PHIÊN ĐẤU GIÁ -> TỰ ĐỘNG RA MÀN HÌNH CHÍNH 
    default void onAuctionCancelled(AuctionCancelledEvent event){}
}
