package com.groupproject.shared.network;
import com.groupproject.shared.model.enums.AuctionStatus;
public class GetAuctionRequest extends Request {
    private Integer sellerId;
    private AuctionStatus status;
    private Integer categoryId;

    // Bắt buộc phải có constructor rỗng để chuẩn hóa (Serialization) khi gửi qua mạng
    public GetAuctionRequest() {}

    // Constructor chính
    public GetAuctionRequest(Integer sellerId, AuctionStatus status, Integer categoryId) {
        this.sellerId = sellerId;
        this.status = status;
        this.categoryId = categoryId;
    }

    // ==============================================================================
    // CÁC HÀM TẠO NHANH (STATIC FACTORY METHODS) ĐỂ DÙNG Ở CLIENT
    // ==============================================================================

    /**
     * Dùng cho HomeController: Lấy các phiên theo Trạng thái (thường là ACTIVED), có thể kèm theo Category
     */
    public static GetAuctionRequest getByStatus(AuctionStatus status, Integer categoryId) {
        // sellerId = null -> Không quan tâm ai bán
        return new GetAuctionRequest(null, status, categoryId);
    }

    /**
     * Dùng cho YourAuctionsController: Lấy các phiên CỦA MÌNH, có thể kèm theo Category
     */
    public static GetAuctionRequest getBySeller(Integer sellerId, Integer categoryId) {
        // status = null -> Lấy cả đang bán, chờ bán, đã kết thúc
        return new GetAuctionRequest(sellerId, null, categoryId);
    }

    /**
     * Dùng cho Admin hoặc tìm kiếm toàn cục: Lấy tất cả
     */
    public static GetAuctionRequest getAll() {
        return new GetAuctionRequest(null, null, null);
    }

    // ==============================================================================
    // GETTERS
    // ==============================================================================
    public Integer getSellerId() { return sellerId; }
    public AuctionStatus getStatus() { return status; }
    public Integer getCategoryId() { return categoryId; }
    
}
    
