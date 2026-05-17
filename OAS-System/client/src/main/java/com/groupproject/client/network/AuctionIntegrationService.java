package com.groupproject.client.network;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.shared.network.AuctionEvent.AuctionEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionListener;
import com.groupproject.shared.network.BidResponse;
// CHỊU TRÁCH NHIỆM THÔNG BÁO ĐẾN TỪNG ROOMS
public class AuctionIntegrationService {
    private final AuctionListener uiListener;
    private final int auctionId;
    public AuctionIntegrationService(int auctionId,AuctionListener uiListener) {
        this.uiListener = uiListener;
        this.auctionId = auctionId;
    }
    // HÀM NGHE THÔNG BÁO 
    public void startListening() {
        EventRouter router= EventRouter.getInstance();
        router.on(BidResponse.class, response ->{
            if (response.getAuctionId() == this.auctionId) {
                if (!response.isSuccess()) {
                    AlertUtils.showError("Error !", response.getMessage());
                }
            }
            else {
                AlertUtils.showError("Error", "Auction Id is different !");
            }
            
        });
        // PHẦN NHẬN THÔNG BÁO THÔNG BÁO CHUNG (CHƯA CẦN BIẾT KẾT QUẢ)
        AuctionEventBus.getInstance().subscribe(auctionId, uiListener);
    }
    // KHI HỌ KHÔNG CÒN TRONG MỘT PHÒNG 
    public void stopListening() {
        AuctionEventBus.getInstance().unsubscribe(auctionId, uiListener);
    }
}
