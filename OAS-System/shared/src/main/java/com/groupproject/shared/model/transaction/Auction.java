    package com.groupproject.shared.model.transaction;

    import java.time.LocalDateTime;
    import java.util.Map;

    import com.groupproject.shared.model.base.Entity;
    import com.groupproject.shared.model.categories.Category;
    import com.groupproject.shared.model.enums.AuctionStatus;

    public class Auction extends Entity {
        private static final long serialVersionUID = 1L;

        private int sellerId; // ID của người bán
        private String title; // Tên sản phẩm
        private String description; // Mô tả sản phẩm
        private Category category; // Danh mục sản phẩm
        private double startingPrice;
        Map<Integer, Map<String, String>> categoryGroupedSpecs;
        private double currentBid; // Giá hiện tại của sản phẩm trong phiên đấu giá
        private Integer highestBidderId; // ID của người đang có giá cao nhất
        private LocalDateTime startTime; // Ngày bắt đầu của phiên đấu giá
        private LocalDateTime endTime; // Ngày kết thúc của phiên đấu giá
        private AuctionStatus status; // Trạng thái của phiên đấu giá (ví dụ: "active", "closed", "cancelled", ...)

        /**
         * Constructor chính - Sử dụng cho tất cả các trường hợp 
         * (Tạo mới từ Request hoặc Load từ Database)
         */
        public Auction(int id, int sellerId, String title, String description, Category category, 
                    Map<Integer, Map<String, String>> categoryGroupedSpecs, double startingPrice, 
                    LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
            super(id); 
            this.sellerId = sellerId;
            this.title = title;
            this.description = description;
            this.category = category;
            this.categoryGroupedSpecs = categoryGroupedSpecs;
            this.startingPrice = startingPrice;
            this.startTime = startTime; // Có thể null nếu là WAITING [cite: 112]
            this.endTime = endTime;
            this.status = status; // Nhận trực tiếp từ tham số [cite: 114]
        }

        public int getSellerId() { return sellerId; }

        public String getTitle() { return title;}

        public String getDescription() { return description; }

        public Category getCategory() { return category; }

        public Map<Integer, Map<String, String>> getCategoryGroupedSpecs() { return categoryGroupedSpecs; }

        public double getCurrentBid() { return currentBid; }
        public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }

        public Integer getHighestBidderId() { return highestBidderId; }
        public void setHighestBidderId(Integer highestBidderId) { this.highestBidderId = highestBidderId; }

        public double getStartingPrice() { return startingPrice; }

        // Chỉ cho phép setStartTime khi chuyển từ WAITING -> ACTIVED, hoặc khi Manager kích hoạt SCHEDULED
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getStartTime() { return startTime; }

        public LocalDateTime getEndTime() { return endTime; }

        public AuctionStatus getStatus() { return status; }

        public void setStatus(AuctionStatus status) { this.status = status; }
    }
