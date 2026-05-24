package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.CreateAuctionRequest;

public class AuctionDAO {

    // 1. TẠO ĐẤU GIÁ MỚI
    public static synchronized Auction createAuction(int sellerId, String title, String description, Category category, 
                                                     Map<Integer, Map<String, String>> categoryGroupedSpecs, 
                                                     double startingPrice, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        
        String auctionSql = "INSERT INTO auctions (seller_id, title, description, category_id, starting_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String specSql = "INSERT INTO auction_specifications (auction_id, category_id, field_name, field_value) VALUES (?, ?, ?, ?)";
        
        boolean originalAutoCommit = true;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            if (conn == null) return null;

            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(auctionSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement specPstmt = conn.prepareStatement(specSql)) {
                
                pstmt.setInt(1, sellerId);
                pstmt.setString(2, title);
                pstmt.setString(3, description);
                pstmt.setInt(4, category.getId());
                pstmt.setDouble(5, startingPrice);
                
                // Xử lý startTime nullable
                if (startTime != null) {
                    pstmt.setString(6, startTime.toString());
                } else {
                    pstmt.setNull(6, Types.VARCHAR);
                }
                
                pstmt.setString(7, endTime.toString());
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
                        // Trả về đúng 10 tham số cho constructor
                        return new Auction(newAuctionId, sellerId, title, description, category, categoryGroupedSpecs, startingPrice, startTime, endTime, status);
                    }
                }
            } catch (SQLException ex) {
                conn.rollback();
                ServerLogger.error("Rollback do lỗi tạo đấu giá: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            ServerLogger.error("Lỗi kết nối DB: " + e.getMessage());
        }
        return null;
    }

    // 2. CHUYỂN ĐỔI REQUEST THÀNH ĐỐI TƯỢNG (Overload)
    public static synchronized Auction createAuction(CreateAuctionRequest request) {
        AuctionStatus status = (request.getStatus() != null) ? request.getStatus() : AuctionStatus.WAITING;

        LocalDateTime parsedStartTime = null;
        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            parsedStartTime = LocalDateTime.parse(request.getStartTime());
        }

        return createAuction(
            request.getSellerId(), request.getTitle(), request.getDescription(), 
            request.getCategory(), request.getCategoryGroupedSpecs(), 
            request.getStartingPrice(), parsedStartTime, 
            LocalDateTime.parse(request.getEndTime()), status
        );
    }

    // 3. LẤY TẤT CẢ (Lịch sử)
    public static List<Auction> getAuctions() {
        return fetchAuctionsFromSql("SELECT * FROM auctions", null);
    }

    // 4. LẤY CÁC PHIÊN ĐANG "SỐNG" (Cho UI chính và Manager)
    // Bao gồm: WAITING (để hiện nút Start), SCHEDULED (để chờ kích hoạt), ACTIVED (đang đấu giá)
    public static List<Auction> getActiveAuctions() {
        String sql = "SELECT * FROM auctions WHERE status IN ('WAITING', 'SCHEDULED', 'ACTIVED') AND end_time > ?";
        return fetchAuctionsFromSql(sql, LocalDateTime.now().toString());
    }

    // 5. CẬP NHẬT TRẠNG THÁI (Dùng cho nút "Start Now" hoặc Manager khi đến giờ)
    public static boolean updateAuctionStatus(int auctionId, AuctionStatus newStatus, LocalDateTime newStartTime) {
        String sql = "UPDATE auctions SET status = ?, start_time = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus.name());
            pstmt.setString(2, (newStartTime != null) ? newStartTime.toString() : null);
            pstmt.setInt(3, auctionId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            ServerLogger.error("Lỗi cập nhật trạng thái: " + e.getMessage());
            return false;
        }
    }

    // 6. CẬP NHẬT TRẠNG THÁI (Chỉ đổi status, dùng cho việc Hủy đấu giá)
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

    // Lấy một danh sách các phiên đấu giá của người bán (có thể dùng cho trang quản lý đấu giá của người bán)
    public static List<Auction> getAuctionsBySeller(int sellerId) {
        String sql = "SELECT * FROM auctions WHERE seller_id = ?";
        // Gọi hàm helper phiên bản nhận tham số int
        return fetchAuctionsFromSql(sql, sellerId); 
    }

    // LẤY MỘT PHIÊN ĐẤU GIÁ THEO ID
    public static Auction getAuctionById(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        // Tận dụng lại hàm helper bạn đã viết sẵn
        List<Auction> result = fetchAuctionsFromSql(sql, auctionId); 
        
        // Nếu danh sách trả về không rỗng, lấy phần tử đầu tiên, ngược lại trả về null
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    // Hàm phụ để tránh lặp code (Helper method)
    private static List<Auction> fetchAuctionsFromSql(String sql, String timeParam) {
        List<Auction> list = new ArrayList<>();
        Map<Integer, Category> categoryMap = CategoryDAO.getCategories();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (timeParam != null) pstmt.setString(1, timeParam);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int catId = rs.getInt("category_id");
                    
                    String sTimeStr = rs.getString("start_time");
                    String eTimeStr = rs.getString("end_time");
                    
                    LocalDateTime sTime = (sTimeStr != null) ? LocalDateTime.parse(sTimeStr) : null;
                    LocalDateTime eTime = (eTimeStr != null) ? LocalDateTime.parse(eTimeStr) : null;
                    AuctionStatus status = AuctionStatus.valueOf(rs.getString("status").toUpperCase());

                    Auction auction = new Auction(
                        id, rs.getInt("seller_id"), rs.getString("title"), 
                        rs.getString("description"), categoryMap.get(catId),
                        getAuctionSpecifications(id, conn), rs.getDouble("starting_price"),
                        sTime, eTime, status // ĐỦ 10 THAM SỐ
                    );

                    auction.setCurrentBid(rs.getDouble("current_bid"));
                    int bidderId = rs.getInt("current_bidder_id");
                    auction.setHighestBidderId(rs.wasNull() ? null : bidderId);

                    list.add(auction);
                }
            }
        } catch (SQLException e) {
            ServerLogger.error("Lỗi fetch auction: " + e.getMessage());
        }
        return list;
    }

    // Hàm helper nạp chồng (Overload) để xử lý các truy vấn dùng tham số kiểu int (như seller_id, category_id,...)
    private static List<Auction> fetchAuctionsFromSql(String sql, int intParam) {
        List<Auction> list = new ArrayList<>();
        Map<Integer, Category> categoryMap = CategoryDAO.getCategories();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Truyền tham số kiểu int vào dấu ? đầu tiên
            pstmt.setInt(1, intParam);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int catId = rs.getInt("category_id");
                    
                    String sTimeStr = rs.getString("start_time");
                    String eTimeStr = rs.getString("end_time");
                    
                    LocalDateTime sTime = (sTimeStr != null) ? LocalDateTime.parse(sTimeStr) : null;
                    LocalDateTime eTime = (eTimeStr != null) ? LocalDateTime.parse(eTimeStr) : null;
                    AuctionStatus status = AuctionStatus.valueOf(rs.getString("status").toUpperCase());

                    Auction auction = new Auction(
                        id, rs.getInt("seller_id"), rs.getString("title"), 
                        rs.getString("description"), categoryMap.get(catId),
                        getAuctionSpecifications(id, conn), rs.getDouble("starting_price"),
                        sTime, eTime, status
                    );

                    auction.setCurrentBid(rs.getDouble("current_bid"));
                    int bidderId = rs.getInt("current_bidder_id");
                    auction.setHighestBidderId(rs.wasNull() ? null : bidderId);

                    list.add(auction);
                }
            }
        } catch (SQLException e) {
            ServerLogger.error("Lỗi fetch auction (int parameter): " + e.getMessage());
        }
        return list;
    }

    private static Map<Integer, Map<String, String>> getAuctionSpecifications(int auctionId, Connection conn) throws SQLException {
        Map<Integer, Map<String, String>> groupedSpecs = new HashMap<>();
        String query = "SELECT category_id, field_name, field_value FROM auction_specifications WHERE auction_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    groupedSpecs.computeIfAbsent(rs.getInt("category_id"), k -> new HashMap<>())
                                .put(rs.getString("field_name"), rs.getString("field_value"));
                }
            }
        }
        return groupedSpecs;
    }
}