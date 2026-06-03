package com.groupproject.shared.model.transaction;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class AuctionDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private Auction auction; // Thông tin gốc của phiên đấu giá
    private String description; 
    private List<BidDTO> bidHistory; // Lịch sử đấu giá (Mới nhất xếp trên)
    private Map<Integer, Map<String, String>> categoryGroupedSpecs;
    private List<String> subImagePaths = new ArrayList<>();
    private List<byte[]> subImageBytesList = new ArrayList<>();


    public AuctionDetail(Auction auction, String description, List<byte[]> subImageBytesList, List<BidDTO> bidHistory, Map<Integer, Map<String, String>> categoryGroupedSpecs) {
        this.auction = auction;
        this.description = description;
        this.subImageBytesList = subImageBytesList;
        this.bidHistory = bidHistory;
        this.categoryGroupedSpecs = categoryGroupedSpecs;
    }

    public Auction getAuction() { return auction; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<BidDTO> getBidHistory() { return bidHistory; }

    public Map<Integer, Map<String, String>> getCategoryGroupedSpecs() { return categoryGroupedSpecs; }
    public void setCategoryGroupedSpecs(Map<Integer, Map<String, String>> specs) { this.categoryGroupedSpecs = specs; }

    public List<String> getSubImagePaths() { return subImagePaths; }
    public void setSubImagePaths(List<String> paths) { this.subImagePaths = paths; }

    public List<byte[]> getSubImageBytesList() { return subImageBytesList; }
    public void setSubImageBytesList(List<byte[]> subImageBytesList) { this.subImageBytesList = subImageBytesList; }
}
