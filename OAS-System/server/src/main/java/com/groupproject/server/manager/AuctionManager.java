package com.groupproject.server.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.groupproject.shared.exception.AuctionClosedException;
import com.groupproject.shared.exception.AuctionNotFoundException;
import com.groupproject.shared.exception.AuthenticationException;
import com.groupproject.shared.exception.InvalidAuctionStateException;
import com.groupproject.shared.exception.InvalidBidException;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;

/** TODO:
 * - Chuyển đổi từ lưu trên RAM sang lưu vào CSDL thông qua DAO (Data Access Object) 
 * tách biệt hoàn toàn với lớp quản lý nghiệp vụ (AuctionManager) ở file auctionManager.java này
 * Tạm thời chưa cần chuyển sang CSDL, vẫn lưu trên RAM để đợi test xong các nghiệp vụ
 * - Cần tạo thêm lớp ClientHandler dựa vào lớp Auction, 
 * BidTransaction, các lớp trong manager và pattern/observer
 * - Lớp ClientHandler cần xử lý các exception từ AuctionManager 
 * và chuyển thành các thông báo lỗi cụ thể gửi về cho client
 * - Lớp ClientHandler cũng sẽ thực thi interface AuctionObserver 
 * để nhận thông báo từ AuctionNotificationManager
 */

/**
 * Quản lý các nghiệp vụ cốt lõi của phiên đấu giá.
 */
public class AuctionManager {

    // Danh sách lưu trữ các phiên đấu giá đang được quản lý trên RAM của Server
    private final Map<String, Auction> auctionMap;

    // Mẫu thiết kế Bill Pugh Singleton 
    private AuctionManager() {
        this.auctionMap = new ConcurrentHashMap<>();
        System.out.println("AuctionManager đã khởi tạo thành công!");
    }

    private static class Helper {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return Helper.INSTANCE;
    }

    // =========================================================================
    // QUẢN LÝ VÒNG ĐỜI (LIFECYCLE) CỦA PHIÊN ĐẤU GIÁ
    // =========================================================================

    /**
     * 1. TẠO ĐẤU GIÁ (WAITING)
     * Người bán tạo nhưng chưa bắt đầu.
     */
    public void createAuction(Auction auction) {
        // Kiểm tra thông tin đầu vào, đảm bảo không null và có ID hợp lệ
        if (auction == null || auction.getId() == null) {
            throw new IllegalArgumentException("Thông tin phiên đấu giá không hợp lệ.");
        }
        
        // Đảm bảo trạng thái ban đầu là WAITING
        auction.setStatus(AuctionStatus.WAITING);
        auctionMap.put(auction.getId(), auction);
        
        System.out.println("Đã tạo phiên đấu giá (WAITING): " + auction.getId());
    }

    /**
     * 2. BẮT ĐẦU ĐẤU GIÁ (ACTIVED)
     * Chuyển trạng thái để người mua có thể bắt đầu đặt giá.
     */
    public void startAuction(String auctionId) throws AuctionNotFoundException, InvalidAuctionStateException {
        Auction auction = getAuctionOrThrow(auctionId);
        
        synchronized (auction) {
            // Chỉ cho phép bắt đầu nếu đang ở trạng thái WAITING
            if (auction.getStatus() == AuctionStatus.WAITING) {
                auction.setStatus(AuctionStatus.ACTIVED);
                System.out.println("Đã bắt đầu phiên đấu giá (ACTIVED): " + auctionId);
            } else {
                throw new InvalidAuctionStateException("Chỉ có thể bắt đầu phiên đấu giá đang ở trạng thái WAITING. Trạng thái hiện tại: " + auction.getStatus());
            }
        }
    }

    /**
     * 3. ĐÓNG PHIÊN ĐẤU GIÁ (ENDED)
     * Hết thời gian, không nhận thêm giá nhưng chưa xóa khỏi bộ nhớ để chờ chốt kết quả.
     */
    public void endAuction(String auctionId) throws AuctionNotFoundException, InvalidAuctionStateException {
        Auction auction = getAuctionOrThrow(auctionId);
        
        synchronized (auction) {
            // Chỉ cho phép đóng nếu đang ở trạng thái ACTIVED
            if (auction.getStatus() == AuctionStatus.ACTIVED) {
                auction.setStatus(AuctionStatus.ENDED);
                System.out.println("Đã đóng phiên đấu giá (ENDED) chờ xử lý: " + auctionId);
                // Gửi thông báo cho mọi người trong phòng: "Đã hết giờ, đang chờ chốt kết quả!"
            } else {
                throw new InvalidAuctionStateException("Chỉ có thể đóng phiên đấu giá đang ở trạng thái ACTIVED. Trạng thái hiện tại: " + auction.getStatus());
            }
        }
    }

    /**
     * 4. HOÀN THÀNH (FINISHED)
     * Đã xác định xong người thắng, xử lý giao dịch xong. Xóa khỏi bộ nhớ đệm (thường là đã lưu DB).
     */
    public void finishAuction(String auctionId) throws AuctionNotFoundException, InvalidAuctionStateException {
        Auction auction = getAuctionOrThrow(auctionId);
        
        synchronized (auction) {
            // Chỉ cho phép hoàn thành nếu đang ở trạng thái ENDED
            if (auction.getStatus() == AuctionStatus.ENDED) {
                auction.setStatus(AuctionStatus.FINISHED);
                auctionMap.remove(auctionId); // Xóa khỏi bộ nhớ sau khi đã hoàn thành và xử lý xong giao dịch
                System.out.println("Đã hoàn thành và chốt giao dịch (FINISHED): " + auctionId);
            } else {
                throw new InvalidAuctionStateException("Chỉ có thể hoàn thành phiên đấu giá đang ở trạng thái ENDED. Trạng thái hiện tại: " + auction.getStatus());
            }
        }
    }

    /**
     * 5. HỦY BỎ (CANCELLED)
     * Người bán hủy giữa chừng.
     */
    public void cancelAuction(String auctionId) throws AuctionNotFoundException, InvalidAuctionStateException {
        Auction auction = getAuctionOrThrow(auctionId);
        
        synchronized (auction) {
            // Chỉ cho phép hủy nếu đang ở trạng thái WAITING hoặc ACTIVED
            if (auction.getStatus() == AuctionStatus.WAITING || auction.getStatus() == AuctionStatus.ACTIVED) {
                auction.setStatus(AuctionStatus.CANCELLED);
                auctionMap.remove(auctionId);
                System.out.println("Đã hủy phiên đấu giá (CANCELLED): " + auctionId);
            } else {
                throw new InvalidAuctionStateException("Chỉ có thể hủy phiên đấu giá đang ở trạng thái WAITING hoặc ACTIVED. Trạng thái hiện tại: " + auction.getStatus());
            }
        }
    }

    // =========================================================================
    // NGHIỆP VỤ ĐẶT GIÁ (BIDDING)
    // =========================================================================

    public void placeBid(String auctionId, double bidAmount, String bidderId) 
            throws AuthenticationException, AuctionClosedException, InvalidBidException, AuctionNotFoundException {
        
        // Kiểm tra xác thực người dùng (đơn giản chỉ kiểm tra bidderId không null, có thể mở rộng sau này)
        if (bidderId == null) {
            throw new AuthenticationException("Lỗi xác thực: Người dùng không hợp lệ.");
        }

        Auction auction = getAuctionOrThrow(auctionId);

        synchronized (auction) {
            // Kiểm tra tính hợp lệ của giá đặt (phải cao hơn giá hiện tại và phiên đấu giá phải đang diễn ra)
            validateBid(auction, bidAmount);

            // Cập nhật giá thầu mới và người đặt cao nhất
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
            
            System.out.println("Thành công: User " + bidderId + " dẫn đầu phiên " + auctionId + " với giá " + bidAmount);
            
            // Gọi Notification Manager ở đây...
        }
    }

    private void validateBid(Auction auction, double bidAmount) 
            throws AuctionClosedException, InvalidBidException {
        
        // Cập nhật logic theo Enum: Chỉ nhận giá khi trạng thái là ACTIVED
        if (auction.getStatus() != AuctionStatus.ACTIVED) {
            String msg = "Không thể đặt giá. Trạng thái hiện tại: " + auction.getStatus();
            if (auction.getStatus() == AuctionStatus.WAITING) {
                msg = "Phiên đấu giá chưa bắt đầu.";
            } else if (auction.getStatus() == AuctionStatus.ENDED) {
                msg = "Phiên đấu giá đã kết thúc, đang chờ chốt kết quả.";
            }
            throw new AuctionClosedException(msg);
        }

        // Giá đặt phải cao hơn giá hiện tại (nếu có) hoặc giá khởi điểm nếu chưa có ai đặt giá nào
        // Nếu currentBid = 0 thì nghĩa là chưa có ai đặt giá nào, lúc này sẽ so sánh với startingPrice
        // Nếu currentBid > 0 thì nghĩa là đã có người đặt giá, lúc này sẽ so sánh với currentBid
        double currentHighest = (auction.getCurrentBid() > 0) ? auction.getCurrentBid() : auction.getStartingPrice();

        if (bidAmount <= currentHighest) {
            throw new InvalidBidException("Giá đặt (" + bidAmount + ") phải lớn hơn giá hiện tại (" + currentHighest + ").");
        }
    }

    // =========================================================================
    // TIỆN ÍCH (UTILITIES)
    // =========================================================================

    public Auction getAuction(String auctionId) {
        return auctionMap.get(auctionId);
    }

    /**
     * Hàm phụ trợ gom chung logic kiểm tra Null
     */
    private Auction getAuctionOrThrow(String auctionId) throws AuctionNotFoundException {
        Auction auction = auctionMap.get(auctionId);
        if (auction == null) {
            throw new AuctionNotFoundException("Lỗi: Không tìm thấy phiên đấu giá " + auctionId + " trong hệ thống.");
        }
        return auction;
    }
}