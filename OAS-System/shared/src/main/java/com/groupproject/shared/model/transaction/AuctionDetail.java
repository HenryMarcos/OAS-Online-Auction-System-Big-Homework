package com.groupproject.shared.model.transaction;
import java.io.Serializable;
import java.util.List;
public class AuctionDetail implements Serializable {
    private static final long serialVersionUID = 1L;
    private Auction auction; // Thông tin gốc của phiên đấu giá
    private List<BidTransaction> bidHistory; // Lịch sử đấu giá (Mới nhất xếp trên)

    public AuctionDetail(Auction auction, List<BidTransaction> bidHistory) {
        this.auction = auction;
        this.bidHistory = bidHistory;
    }

    public Auction getAuction() { return auction; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
}
