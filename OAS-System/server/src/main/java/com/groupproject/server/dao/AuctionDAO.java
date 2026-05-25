package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.request.CreateAuctionRequest;

public class AuctionDAO {

    // =========================================================================
    // 1. NHÓM TẠO MỚI (CREATE ACTIONS)
    // =========================================================================

    /**
     * Chuyển đổi Request từ Client thành đối tượng Auction và lưu vào DB
     */
    public static synchronized Auction createAuction(CreateAuctionRequest request) {
        int sellerId = ClientContext.currentUser.get().getId();
        AuctionStatus status = (request.getStatus() != null) ? request.getStatus() : AuctionStatus.WAITING;

        return createAuction(
            sellerId, request.getTitle(), request.getDescription(), 
            request.getCategory(), request.getCategoryGroupedSpecs(), 
            request.getStartingPrice(), 
            parseDateTimeSafely(request.getStartTime()), 
            parseDateTimeSafely(request.getEndTime()), 
            status
        );
    }

    /**
     * Hàm lõi thực hiện INSERT vào bảng auctions và auction_specifications (Dùng Transaction)
     */
    public static synchronized Auction createAuction(int sellerId, String title, String description, Category category, 
                                                     Map<Integer, Map<String, String>> categoryGroupedSpecs, 
                                                     double startingPrice, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        
        String auctionSql = "INSERT INTO auctions (seller_id, title, description, category_id, starting_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String specSql = "INSERT INTO auction_specifications (auction_id, category_id, field_name, field_value) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            if (conn == null) return null;

            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            try (PreparedStatement pstmt = conn.prepareStatement(auctionSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement specPstmt = conn.prepareStatement(specSql)) {
                
                pstmt.setInt(1, sellerId);
                pstmt.setString(2, title);
                pstmt.setString(3, description);
                pstmt.setInt(4, category.getId());
                pstmt.setDouble(5, startingPrice);
                pstmt.setTimestamp(6, startTime != null ? Timestamp.valueOf(startTime) : null);
                pstmt.setTimestamp(7, Timestamp.valueOf(endTime));
                pstmt.setString(8, status.name());

                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newAuctionId = rs.getInt(1);
                        if (categoryGroupedSpecs != null) {
                            for (var categoryEntry : categoryGroupedSpecs.entrySet()) {
                                for (var fieldEntry : categoryEntry.getValue().entrySet()) {
                                    specPstmt.setInt(1, newAuctionId);
                                    specPstmt.setInt(2, categoryEntry.getKey());
                                    specPstmt.setString(3, fieldEntry.getKey());
                                    specPstmt.setString(4, fieldEntry.getValue());
                                    specPstmt.addBatch();
                                }
                            }
                            specPstmt.executeBatch();
                        }
                        conn.commit();
                        return new Auction(newAuctionId, sellerId, title, description, category, categoryGroupedSpecs, startingPrice, startTime, endTime, status);
                    }
                }
            } catch (SQLException ex) {
                conn.rollback();
                ServerLogger.error("Rollback tạo đấu giá: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            ServerLogger.error("Lỗi kết nối DB khi tạo auction: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // 2. NHÓM TRUY VẤN DANH SÁCH (READ ACTIONS - LIST)
    // =========================================================================

    public static List<Auction> getAuctions() {
        return fetchAuctionsFromSql("SELECT * FROM auctions", null);
    }

    public static List<Auction> getActiveAuctions() {
        // Lấy các phiên còn hạn và ở trạng thái có thể tương tác
        String sql = "SELECT * FROM auctions WHERE status IN ('WAITING', 'SCHEDULED', 'ACTIVED') AND end_time > ?";
        return fetchAuctionsFromSql(sql, Timestamp.valueOf(LocalDateTime.now()));
    }

    public static List<Auction> getAuctionsBySeller(int sellerId) {
        String sql = "SELECT * FROM auctions WHERE seller_id = ?";
        return fetchAuctionsFromSql(sql, sellerId); 
    }

    public static Auction getAuctionById(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        List<Auction> result = fetchAuctionsFromSql(sql, auctionId); 
        return (result != null && !result.isEmpty()) ? result.get(0) : null;
    }

    // =========================================================================
    // 3. NHÓM CẬP NHẬT TRẠNG THÁI (UPDATE ACTIONS)
    // =========================================================================

    /**
     * Cập nhật cả Status và StartTime (Dùng khi bắt đầu phiên đấu giá)
     */
    public static boolean updateAuctionStatus(int auctionId, AuctionStatus newStatus, LocalDateTime newStartTime) {
        String sql = "UPDATE auctions SET status = ?, start_time = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus.name());
            pstmt.setTimestamp(2, (newStartTime != null) ? Timestamp.valueOf(newStartTime) : null);
            pstmt.setInt(3, auctionId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            ServerLogger.error("Lỗi cập nhật trạng thái & thời gian: " + e.getMessage());
            return false;
        }
    }

    /**
     * Chỉ cập nhật duy nhất trạng thái (Dùng khi Hủy hoặc Kết thúc)
     */
    public static boolean updateAuctionStatusOnly(int auctionId, AuctionStatus newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, auctionId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            ServerLogger.error("Lỗi cập nhật trạng thái (chỉ status): " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 4. NHÓM PHỤC VỤ HOUSEKEEPING (MAINTENANCE QUERIES)
    // =========================================================================

    public static List<Integer> getExpiredWaitingAuctions(LocalDateTime threshold) {
        return fetchIdsByCondition("SELECT id FROM auctions WHERE status = 'WAITING' AND start_time < ?", threshold);
    }

    public static List<Integer> getMissedScheduledAuctions(LocalDateTime now) {
        return fetchIdsByCondition("SELECT id FROM auctions WHERE status = 'SCHEDULED' AND start_time < ?", now);
    }

    public static List<Integer> getExpiredActiveAuctions(LocalDateTime now) {
        return fetchIdsByCondition("SELECT id FROM auctions WHERE status = 'ACTIVED' AND end_time < ?", now);
    }

    private static List<Integer> fetchIdsByCondition(String sql, LocalDateTime time) {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(time));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id"));
        } catch (SQLException e) {
            ServerLogger.error("DAO Maintenance Error: " + e.getMessage());
        }
        return ids;
    }

    // =========================================================================
    // 5. CÁC HÀM TRỢ GIÚP NỘI BỘ (INTERNAL HELPERS - Tối ưu 1+N Query)
    // =========================================================================

    private static List<Auction> fetchAuctionsFromSql(String sql, Object param) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (param instanceof Timestamp) pstmt.setTimestamp(1, (Timestamp) param);
            else if (param instanceof Integer) pstmt.setInt(1, (Integer) param);
            else if (param instanceof String) pstmt.setString(1, (String) param);
            
            return executeAndFetchWithBatchSpecs(pstmt, conn);
        } catch (SQLException e) {
            ServerLogger.error("Lỗi fetch auction: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<Auction> executeAndFetchWithBatchSpecs(PreparedStatement pstmt, Connection conn) throws SQLException {
        List<Auction> list = new ArrayList<>();
        Map<Integer, Category> categoryMap = CategoryDAO.getCategories();
        Map<Integer, Map<Integer, Map<String, String>>> specsByAuctionId = new HashMap<>();

        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Map<Integer, Map<String, String>> emptySpecsMap = new HashMap<>();
                specsByAuctionId.put(id, emptySpecsMap);

                Auction auction = new Auction(
                    id, rs.getInt("seller_id"), rs.getString("title"), 
                    rs.getString("description"), categoryMap.get(rs.getInt("category_id")),
                    emptySpecsMap, rs.getDouble("starting_price"), 
                    parseDateTimeSafely(rs.getString("start_time")), 
                    parseDateTimeSafely(rs.getString("end_time")), 
                    AuctionStatus.valueOf(rs.getString("status").toUpperCase())
                );
                auction.setCurrentBid(rs.getDouble("current_bid"));
                int bidderId = rs.getInt("current_bidder_id");
                auction.setHighestBidderId(rs.wasNull() ? null : bidderId);
                list.add(auction);
            }
        }
        if (!specsByAuctionId.isEmpty()) loadSpecificationsInBatch(specsByAuctionId, conn);
        return list;
    }

    private static void loadSpecificationsInBatch(Map<Integer, Map<Integer, Map<String, String>>> specsByAuctionId, Connection conn) throws SQLException {
        String placeholders = String.join(",", Collections.nCopies(specsByAuctionId.size(), "?"));
        String query = "SELECT auction_id, category_id, field_name, field_value FROM auction_specifications WHERE auction_id IN (" + placeholders + ")";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            int index = 1;
            for (Integer id : specsByAuctionId.keySet()) pstmt.setInt(index++, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int aId = rs.getInt("auction_id");
                    int cId = rs.getInt("category_id");
                    specsByAuctionId.get(aId).computeIfAbsent(cId, k -> new HashMap<>())
                                    .put(rs.getString("field_name"), rs.getString("field_value"));
                }
            }
        }
    }

    private static LocalDateTime parseDateTimeSafely(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) return null;
        try {
            // Thử parse theo chuẩn thông thường (Ví dụ: "2026-05-25T07:40:00")
            return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
        } catch (Exception e) {
            try {
                // SỬA LỖI Ở ĐÂY: Xử lý trường hợp Driver SQLite lưu thành số mili-giây (Unix Epoch)
                long millis = Long.parseLong(dateTimeStr);
                return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault());
            } catch (Exception ex) {
                ServerLogger.error("Không thể dịch ngày tháng từ DB: " + dateTimeStr);
                return null;
            }
        }
    }
}