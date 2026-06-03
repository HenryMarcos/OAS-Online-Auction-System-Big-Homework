package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static java.sql.Types.VARCHAR;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.utils.ImageStorageManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;

public class AuctionDAO {

    // Lấy thông tin nhận được từ database và tạo thành phiên đấu giá
    // --------------------------------------------------------------
    private static Auction extractAuctionFromResultSet(ResultSet rs, Map<Integer, Category> categoryMap) throws SQLException {
        int id = rs.getInt("id");
        int sellerId = rs.getInt("seller_id");
        String title = rs.getString("title");
        int categoryId = rs.getInt("category_id");
        double startingPrice = rs.getDouble("starting_price");
        double currentBid = rs.getDouble("current_bid");
        Integer highestBidderId = (Integer) rs.getObject("current_bidder_id"); 
        String mainImagePath = rs.getString("main_image_path"); 
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

        LocalDateTime startTime = rs.getString("start_time") != null ? LocalDateTime.parse(rs.getString("start_time")) : null;
        LocalDateTime endTime = rs.getString("end_time") != null ? LocalDateTime.parse(rs.getString("end_time")) : null;
        long duration = rs.getLong("duration");

        Category category = categoryMap.get(categoryId);
        
        // 🌟 LOAD BYTES FROM DISK FIRST
        byte[] imageBytes = null;
        if (mainImagePath != null && !mainImagePath.isEmpty()) {
            imageBytes = ImageStorageManager.loadImage(mainImagePath);
        }
        
        // 🌟 CALL NEW CONSTRUCTOR
        Auction auction = new Auction(
            id, sellerId, title, imageBytes, category, 
            startingPrice, duration, startTime, endTime, status
        );

        // 🌟 SET THE REMAINING FIELDS VIA SETTERS
        auction.setMainImagePath(mainImagePath);
        auction.setCurrentBid(currentBid);
        auction.setHighestBidderId(highestBidderId);

        return auction;
    }

    // Tạo phiên đấu giá mới(Lưu dữ liệu và các hình ảnh vào database)
    // ---------------------------------------------------------------
    public static synchronized Auction createAuction(int sellerId, String title, String mainImagePath, List<String> subImagePaths, 
                                                     String description, Category category, Map<Integer, Map<String, String>> categoryGroupedSpecs, 
                                                     double startingPrice, 
                                                     long duration, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        ServerLogger.info("Creating new auction");

        String auctionSql = "INSERT INTO auctions (seller_id, title, main_image_path, description, category_id, starting_price, duration, start_time, end_time, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        

        try (Connection conn = DatabaseManager.INSTANCE.getConnection(); ) {
            if (conn == null) {
                ServerLogger.error("Could not obtain a database connection from the pool.");
                return null;
            }
            // Bắt đầu giao dịch để duy trì tính toàn vẹn của cơ sở dữ liệu
            conn.setAutoCommit(false);

            int newAuctionId = -1;

            // 2. Thêm các thông tin cốt lõi của phiên đấu giá
            try (PreparedStatement pstmt = conn.prepareStatement(auctionSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, sellerId);
                pstmt.setString(2, title);
                pstmt.setString(3, mainImagePath);
                pstmt.setString(4, description);
                pstmt.setInt(5, category.getId());
                pstmt.setDouble(6, startingPrice);
                // Xử lý trường hợp thời gian bị null(trạng thái WAITING chờ bắt đầu)
                if (startTime != null) {
                    pstmt.setString(8, startTime.toString());
                    pstmt.setString(9, endTime.toString());
                } else {
                    pstmt.setNull(8, java.sql.Types.VARCHAR);
                    pstmt.setNull(9, java.sql.Types.VARCHAR);
                }
                
                pstmt.setLong(7, duration);
                pstmt.setString(10, status.name());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    return null;
                }

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) { newAuctionId = rs.getInt(1); }
                }
            } 
            
            // 4. Lưu các field mà người dùng nhập vào
            if (newAuctionId != -1 && categoryGroupedSpecs != null) {
                String specSql = "INSERT INTO auction_specifications (auction_id, category_id, field_name, field_value) VALUES (?, ?, ?, ?)";
                try (PreparedStatement specStmt = conn.prepareStatement(specSql)) {
                    for (Map.Entry<Integer, Map<String, String>> categoryEntry : categoryGroupedSpecs.entrySet()) {
                        int catId = categoryEntry.getKey();
                        for (Map.Entry<String, String> spec : categoryEntry.getValue().entrySet()) {
                            specStmt.setInt(1, newAuctionId);
                            specStmt.setInt(2, catId);
                            specStmt.setString(3, spec.getKey());
                            specStmt.setString(4, spec.getValue());
                            specStmt.addBatch();
                        }
                    }
                    specStmt.executeBatch();
                }

                // Insert Sub-Image Paths
                if (subImagePaths != null && !subImagePaths.isEmpty()) {
                    String subImgSql = "INSERT INTO auction_images (auction_id, image_path) VALUES (?, ?)";
                    try (PreparedStatement imgStmt = conn.prepareStatement(subImgSql)) {
                        for (String path : subImagePaths) {
                            imgStmt.setInt(1, newAuctionId);
                            imgStmt.setString(2, path);
                            imgStmt.addBatch();
                        }
                        imgStmt.executeBatch();
                    }
                }
            }

            conn.commit(); 
            ServerLogger.info("Auction successfully saved with ID: " + newAuctionId);
            
            return getAuctionById(newAuctionId);
    
        } catch (SQLException connectionEx) {
            ServerLogger.error("Database connection level error: " + connectionEx.getMessage());
        }
        return null;
    }

    // Tạo phiên đấu giá mới(nhận dữ liệu dưới dạng Request)
    // -----------------------------------------------------
    /* 
    public static synchronized Auction createAuction(CreateAuctionRequest request, ClientHandler clientContext) {
        AuctionStatus parsedStatus;
        parsedStatus = request.getStatus() != null ? request.getStatus() : (request.getStartTime() != null ? AuctionStatus.SCHEDULED : AuctionStatus.WAITING); 
        if (parsedStatus == AuctionStatus.WAITING) {
            ServerLogger.info("Creating auction with WAITING status");
        } else { ServerLogger.info("Creating auction with ACTIVATED status"); }


        return createAuction(clientContext.getAuthenticatedUserId(), request.getTitle(), 
                             request.getMainImageBytes(), request.getSubImagesBytes(),
                             request.getDescription(), 
                             request.getCategory(), request.getCategoryGroupedSpecs(), request.getStartingPrice(), 
                             request.getDuration(), request.getStartTime(), request.getEndTime(),
                             parsedStatus);
    }
                             */

    // Lấy các phiên đấu giá phục vụ cho tính năng xem lịch sử đâu giá
    // Lấy hết các phiên đấu giá kể cả đã kết thúc
    // ---------------------------------------------------------------
    public static List<Auction> getAuctions() {
        List<Auction> auctionList = new ArrayList<>();
        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();
        
        String sql = "SELECT * FROM auctions";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while (rs.next()) {
                Auction auction = extractAuctionFromResultSet(rs, categoryMap);
                auctionList.add(auction);
            }

        } catch (SQLException e) {
            ServerLogger.error("Database error getting all auctions: " + e.getMessage());
        }
        return auctionList;
    }

    // Lấy các phiên đấu giá theo trạng thái của chúng
    // -----------------------------------------------
    public static List<Auction> getAuctionsByStatus(AuctionStatus status) {
        List<Auction> auctionList = new ArrayList<>();
        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();
        
        String sql = "SELECT * FROM auctions WHERE status = ?";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, status.name());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = extractAuctionFromResultSet(rs, categoryMap);
                    auctionList.add(auction);
                }
            }

        } catch (SQLException e) {
            ServerLogger.error("Database error getting auctions by status: " + e.getMessage());
        }
        return auctionList;
    }

    // Lấy các phiên đấu giá theo id người bán
    // ---------------------------------------
    public static List<Auction> getAuctionsBySellerId(int sellerId, Integer categoryId) {
        List<Auction> auctionList = new ArrayList<>();
        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();

        String sql = (categoryId != null)
            ? "SELECT * FROM auctions WHERE seller_id = ? AND category_id = ? ORDER BY id DESC"
            : "SELECT * FROM auctions WHERE seller_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sellerId);
            if (categoryId != null) pstmt.setInt(2, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) auctionList.add(extractAuctionFromResultSet(rs, categoryMap));
            }
        } catch (SQLException e) { ServerLogger.error("Error getting auctions by seller: " + e.getMessage()); }
        return auctionList;
    }

    // Lấy phiên đấu giá theo id của chúng
    // -----------------------------------
    public static Auction getAuctionById(int auctionId) {
        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();
        String sql = "SELECT * FROM auctions WHERE id = ?";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, auctionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Auction auction = extractAuctionFromResultSet(rs, categoryMap);
                    
                    return auction;
                }
            }
        } catch (SQLException e) {
            ServerLogger.error("Error fetching auction by ID: " + e.getMessage());
        }
        return null;
    }

    // Lấy Description của Auction
    public static String getAuctionDescription(int auctionId) {
        String sql = "SELECT description FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("description");
            }
        } catch (Exception e) { ServerLogger.error("Error fetching description: " + e.getMessage()); }
        return "";
    }

    // Lấy danh sách tên file ảnh phụ từ Database (Giả sử có bảng auction_images)
    // ------------------------------------------------------------------------------
    public static List<String> getSubImagePaths(int auctionId) {
        List<String> paths = new ArrayList<>();
        String sql = "SELECT image_path FROM auction_images WHERE auction_id = ?";
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) paths.add(rs.getString("image_path"));
            }
        } catch (Exception e) { ServerLogger.error("Error fetching sub images: " + e.getMessage()); }
        return paths;
    }

    // Lấy thông tin về các field người dùng đã nhập vào khi tạo phiên đấu giá
    // -----------------------------------------------------------------------
    public static Map<Integer, Map<String, String>> getSpecificationsForAuction(int auctionId) {
        Map<Integer, Map<String, String>> specs = new HashMap<>();
        // Lấy dữ liệu specs từ bảng auction_specifications cho auctionId này
        String sql = "SELECT * FROM auction_specifications WHERE auction_id = ?";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Populate your Map based on your database structure
                    // Example logic (adapt to your exact DB column names):
                    int categoryId = rs.getInt("category_id");
                    String fieldName = rs.getString("field_name");
                    String fieldValue = rs.getString("field_value");
                    
                    specs.putIfAbsent(categoryId, new HashMap<>());
                    specs.get(categoryId).put(fieldName, fieldValue);
                }
            }
        } catch (SQLException e) {
            ServerLogger.error("Error fetching specifications: " + e.getMessage());
        }
        return specs;
    }

    // Update thông tin về bid mới
    // ---------------------------
    public static boolean updateBid(int auctionId, int bidderId, double bidAmount) {
        String updateSql = "UPDATE auctions SET current_bid = ?, current_bidder_id = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql);) {
            
            updateStmt.setDouble(1, bidAmount);
            updateStmt.setInt(2, bidderId);
            updateStmt.setInt(3, auctionId);

            return updateStmt.executeUpdate() > 0;
        } catch (Exception e) {
            ServerLogger.error("Database error updating bid: " + e.getMessage());
            return false;
        }
    }

    // Update trạng thái phiên đấu giá(dùng sau khi kết thúc)
    // ------------------------------------------------------
    public static boolean updateAuctionStatus(int auctionId, AuctionStatus status) {
        String updateSql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
             
            updateStmt.setString(1, status.name());
            updateStmt.setInt(2, auctionId);

            return updateStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            ServerLogger.error("Error updating status: " + e.getMessage());
            return false;        }
    }

    public static boolean updateAuctionStatusWithTime(int auctionId, AuctionStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "UPDATE auctions SET status = ?, start_time = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            if (startTime != null) { pstmt.setString(2, startTime.toString()); }
            else { pstmt.setNull(2, VARCHAR); }
            if (endTime != null) { pstmt.setString(3, endTime.toString()); }
            else { pstmt.setNull(3, VARCHAR); }
            pstmt.setInt(4, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            ServerLogger.error("Error updating status with time: " + e.getMessage());
            return false;
        }
    }

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
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, time.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id"));
        } catch (SQLException e) {
            ServerLogger.error("DAO Maintenance Error: " + e.getMessage());
        }
        return ids;
    }
}
