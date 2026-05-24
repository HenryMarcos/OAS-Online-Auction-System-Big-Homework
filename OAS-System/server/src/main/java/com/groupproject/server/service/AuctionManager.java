package com.groupproject.server.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.AuctionItem;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.BidRequest;

public class AuctionManager {
    // Tìm nhanh các phiên đấu giá còn đang hoạt động
    private final ConcurrentHashMap<Integer, Auction> activeAuctions = new ConcurrentHashMap<>();

    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // 1. Constructor private để ngăn chặn việc khởi tạo từ bên ngoài
    private AuctionManager() {
        // Khi khởi tạo, load tất cả các phiên đấu giá đang hoạt động từ database vào bộ nhớ
        loadActiveAuctionsFromDatabase();
    }

    // 2. Static inner class chứa instance duy nhất (The Bill Pugh concept)
    private static class AuctionManagerHelper {
        // Biến INSTANCE được khởi tạo và gán là final
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    // 3. Phương thức lấy instance hoàn toàn không cần 'synchronized'
    public static AuctionManager getInstance() {
        return AuctionManagerHelper.INSTANCE;
    }

    private void loadActiveAuctionsFromDatabase() {
        // Chỉ lấy những cái thực sự cần thiết từ Database
        for (Auction auction : AuctionDAO.getActiveAuctions()) {
            registerAuction(auction);
        }
    }

    // Thêm một phiên đấu giá mới vào hệ thống có status là WAITING, SCHEDULED (dùng khi tạo mới hoặc load từ DB)
    public void registerAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        long now = System.currentTimeMillis();

        if (auction.getStatus() == AuctionStatus.ACTIVED) {
            // --- LOGIC KẾT THÚC --- (Đếm ngược đến endTime chuyển từ ACTIVED -> ENDED)
            long delayEnd = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
            scheduler.schedule(() -> finishAuction(auction.getId()), Math.max(0, delayEnd), TimeUnit.SECONDS);
            
        } else if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            // --- LOGIC BẮT ĐẦU TỰ ĐỘNG --- (Đếm ngược đến startTime chuyển từ SCHEDULED -> ACTIVED)
            long delayStart = Duration.between(LocalDateTime.now(), auction.getStartTime()).toSeconds();
            
            scheduler.schedule(() -> {
                startAuction(auction.getId());
            }, Math.max(0, delayStart), TimeUnit.SECONDS);
            
            ServerLogger.info("Auction " + auction.getId() + " is SCHEDULED. Start timer set in " + delayStart + "s.");
        }
    }

    /**
     * Dùng khi người bán tự bấm nút "Start Now" (Chuyển WAITING -> ACTIVED)
     * Hàm này được gọi TỪ ChangeAuctionStatusHandler.
     */
    public void activateWaitingAuction(Auction updatedAuction) {
        // 1. Cập nhật lại đối tượng Auction mới nhất (đã có startTime và status = ACTIVED) vào bộ nhớ RAM
        activeAuctions.put(updatedAuction.getId(), updatedAuction);

        // 2. Tính toán thời gian từ "bây giờ" đến lúc kết thúc (endTime)
        long delayEnd = Duration.between(LocalDateTime.now(), updatedAuction.getEndTime()).toSeconds();
        if (delayEnd < 0) { delayEnd = 0; }

        // 3. Đưa nhiệm vụ "Đóng phiên đấu giá" vào hàng đợi của ScheduledExecutorService
        scheduler.schedule(() -> {
            finishAuction(updatedAuction.getId());
        }, delayEnd, TimeUnit.SECONDS);

        ServerLogger.info("Auction " + updatedAuction.getId() + " manually ACTIVATED. End timer set in " + delayEnd + "s.");
        
        // (Tuỳ chọn) Chỗ này sau này bạn có thể gọi ClientManager để gửi Broadcast 
        // thông báo cho tất cả Client khác là: "Ê, có phiên đấu giá mới vừa mở nè!"
    }

    /**
     * Dùng khi hệ thống ép kết thúc một phiên WAITING do bấm Start quá trễ
     */
    public void forceEndWaitingAuction(int auctionId) {
        // Lấy và xóa luôn khỏi danh sách đang chờ trên RAM
        Auction auction = activeAuctions.remove(auctionId);
        
        if (auction != null) {
            auction.setStatus(AuctionStatus.ENDED);
            ServerLogger.info("Auction " + auctionId + " forced to END because it was started too late.");
            
            // TODO: Thông báo cho người bán biết phiên đấu giá đã bị hủy do quá hạn
        }
    }

    /**
     * Dùng khi người bán hoặc Admin HỦY phiên đấu giá
     */
    public void cancelAuction(int auctionId) {
        // Gỡ khỏi RAM để các tiến trình ScheduledExecutorService không thực thi nữa
        Auction auction = activeAuctions.remove(auctionId);
        
        if (auction != null) {
            auction.setStatus(AuctionStatus.CANCELLED);
            ServerLogger.info("Auction " + auctionId + " cancelled and removed from active RAM memory.");
            
            // TODO (Tương lai): Thông báo cho người bán và tất cả người mua đã đặt giá thầu biết phiên đấu giá đã bị hủy
        }
    }

    /**
     * Dùng khi hệ thống TỰ ĐỘNG mở phiên đấu giá đã lên lịch (Chuyển SCHEDULED -> ACTIVED)
     * Hàm này được gọi BỞI chính luồng ngầm của ScheduledExecutorService.
     */
    private void startAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        // Kiểm tra an toàn xem phiên đấu giá có còn tồn tại và đúng là đang SCHEDULED không
        if (auction != null && auction.getStatus() == AuctionStatus.SCHEDULED) {
            LocalDateTime now = LocalDateTime.now();
            
            // 1. Cập nhật xuống Database trước (Dùng hàm updateAuctionStatus đã viết trong DAO)
            boolean isUpdatedInDb = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.ACTIVED, now);
            
            if (isUpdatedInDb) {
                // 2. Cập nhật đối tượng trên RAM
                auction.setStatus(AuctionStatus.ACTIVED);
                auction.setStartTime(now); 
                
                // 3. Lên lịch đếm ngược để ĐÓNG phiên đấu giá này
                long delayEnd = Duration.between(now, auction.getEndTime()).toSeconds();
                if (delayEnd < 0) { delayEnd = 0; }
                
                scheduler.schedule(() -> {
                    finishAuction(auctionId);
                }, delayEnd, TimeUnit.SECONDS);
                
                ServerLogger.info("Auction " + auctionId + " auto-started from SCHEDULED. End timer set in " + delayEnd + "s.");
                
                // (Tuỳ chọn) Broadcast thông báo cho Client...
            } else {
                ServerLogger.error("Failed to auto-start SCHEDULED auction " + auctionId + " in Database.");
            }
        }
    }

    /**
     * BƯỚC 1: Hết giờ đếm ngược (Chuyển ACTIVED -> FINISHED)
     * Khóa phiên đấu giá, chốt sổ và bắt đầu xử lý hậu kỳ.
     */
    private void finishAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        if (auction != null && auction.getStatus() == AuctionStatus.ACTIVED) {
            ServerLogger.info("Time is up! Changing auction " + auctionId + " status to FINISHED...");
            
            // 1. Cập nhật Database: status = FINISHED (Tận dụng hàm updateAuctionStatusOnly bên DAO)
            boolean isFinishedInDb = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.FINISHED);
            
            if (isFinishedInDb) {
                // 2. Cập nhật RAM: Trạng thái này sẽ chặn mọi BidRequest mới gửi tới
                auction.setStatus(AuctionStatus.FINISHED);
                
                // 3. TẠI ĐÂY BẠN SẼ VIẾT LOGIC HẬU KỲ:
                // - Lấy highestBidderId và currentBid để chốt người thắng
                // - Kiểm tra xem có ai bid không (nếu không có thì móm/rớt giá)
                // - Gọi logic tạo hóa đơn (Invoice) hoặc lưu lịch sử
                // - Gửi thông báo (Notification) cho Seller và Buyer
                
                // 4. Chuyển sang bước đóng hoàn toàn (ENDED)
                // (Bạn có thể gọi trực tiếp luôn, hoặc đưa vào scheduler nếu logic hậu kỳ cần thời gian xử lý)
                endAuction(auctionId);
            } else {
                ServerLogger.error("Failed to change auction " + auctionId + " to FINISHED in DB.");
            }
        }
    }

    /**
     * BƯỚC 2: Đóng hoàn toàn phiên đấu giá (Chuyển FINISHED -> ENDED)
     * Hàm này được gọi sau khi mọi thủ tục hậu kỳ đã hoàn tất.
     */
    private void endAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
            ServerLogger.info("Post-processing complete. Closing auction " + auctionId + " (ENDED).");
            
            // 1. Cập nhật Database: status = ENDED
            boolean isEndedInDb = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.ENDED);
            
            if (isEndedInDb) {
                // 2. Cập nhật RAM
                auction.setStatus(AuctionStatus.ENDED);
                
                // 3. XÓA KHỎI BỘ NHỚ RAM (Vì phiên đấu giá đã kết thúc hoàn toàn vòng đời)
                activeAuctions.remove(auctionId);
                
                ServerLogger.info("Auction " + auctionId + " fully ENDED and cleared from RAM.");
            }
        }
    }

    public synchronized boolean placeBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);

        if (auction == null) {
            ServerLogger.warning("Bid rejected: Auction " + auctionId + " is not active or already closed.");
            return false;
        }

        // Kiểm tra bid
        if (bidAmount <= auction.getCurrentBid() || bidAmount < auction.getStartingPrice()) {
            return false;
        }

        // Update trạng thái trong bộ nhớ

        // TODO: Thông báo cho người dùng có trạng thái cao nhất trước

        auction.setCurrentBid(bidAmount);
        auction.setHighestBidderId(bidderId);

        // TODO: Update trạng thái trong database

        return true;
    }

    public synchronized boolean placeBid(BidRequest request) {
        return placeBid(request.getAuctionId(), request.getBidderId(), request.getBidAmount());
    }

    public static synchronized boolean proccessBid(int auctionId, int bidderId, double bidAmount) {
        String checkSql = "SELECT current_bid, is_active FROM auctions WHERE id = ?";
        String updateSql = "UPDATE auctions SET current_bid = ?, highest_bidder = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();) {

            // Kiểm tra xem bid hợp lý chưa
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, auctionId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    double currentBid = rs.getDouble("current_bid");
                    boolean isActive = rs.getBoolean("is_active");

                    // Nếu auction đã kết thúc hoặc bid quá thấp thì từ chối
                    if (!isActive || bidAmount <= currentBid) {
                        ServerLogger.info("USER " + bidderId + ": auction is not active or bid is too low");
                        return false;
                    }
                } else {
                    ServerLogger.info("USER " + bidderId + ": auctionId does not exist");
                    return false;
                }
            }

            // Update bid cho các user
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, bidAmount);
                updateStmt.setInt(2, bidderId);
                updateStmt.setInt(3, auctionId);
                updateStmt.executeQuery();
                return true;
            }

        } catch (Exception e) {
            ServerLogger.error("Database error processing bid: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean proccessBid(BidRequest bidRequest) {
        return proccessBid(bidRequest.getAuctionId(), bidRequest.getBidderId(), bidRequest.getBidAmount());
    }

    public static List<AuctionItem> getActiveAuctions() {
        List<AuctionItem> activeAuctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE is_active = 1";

        try (Connection conn = DatabaseManager.getInstance().getConnection();) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                AuctionItem item = new AuctionItem(
                    rs.getInt("id"),
                    rs.getString("item_name"), 
                    rs.getDouble("current_bid"), 
                    rs.getString("highest_bidd")
                );

                activeAuctions.add(item);
            }
        } catch (Exception e) {
            ServerLogger.error("Error fetching auctions: " + e.getMessage());
        }
        return activeAuctions;
    }
}

