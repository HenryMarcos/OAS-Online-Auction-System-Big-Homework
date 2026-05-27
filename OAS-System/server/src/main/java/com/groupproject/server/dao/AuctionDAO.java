package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import com.groupproject.shared.network.requests.CreateAuctionRequest;

public class AuctionDAO {

    // Lấy thông tin nhận được từ database và tạo thành phiên đấu giá
    // --------------------------------------------------------------
    private static Auction extractAuctionFromResultSet(ResultSet rs, Map<Integer, Category> categoryMap) throws SQLException {
        int id = rs.getInt("id");
        int sellerId = rs.getInt("seller_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
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
        
        Auction auction = new Auction(id, sellerId, title, mainImagePath, new ArrayList<>(), description, category, new HashMap<>(), startingPrice, duration, startTime, endTime, status);
        auction.setCurrentBid(currentBid);
        auction.setHighestBidderId(highestBidderId);

        return auction;
    }

    // Tạo phiên đấu giá mới(Lưu dữ liệu và các hình ảnh vào database)
    // ---------------------------------------------------------------
    public static synchronized Auction createAuction(int sellerId, String title, byte[] mainImageBytes, List<byte[]> subImagesBytes, 
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


            // 1. Lưu file ảnh chính
            String mainImageFileName = null;
            if (mainImageBytes != null && mainImageBytes.length > 0) {
                mainImageFileName = ImageStorageManager.saveImage(mainImageBytes); // e.g., "a5b2-32cf.jpg"
            }

            int newAuctionId = -1;

            // 2. Thêm các thông tin cốt lõi của phiên đấu giá
            try (PreparedStatement pstmt = conn.prepareStatement(auctionSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, sellerId);
                pstmt.setString(2, title);
                pstmt.setString(3, mainImageFileName);
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

                ServerLogger.info("Prepare to execute prepared statement");
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) { newAuctionId = rs.getInt(1); }
                }
            } 

            // 3. Lưu thư viện ảnh(ảnh phụ)
            if (newAuctionId != -1 && subImagesBytes != null && !subImagesBytes.isEmpty()) {
                String subImgSql = "INSERT INTO auction_images (auction_id, image_path) VALUES (?, ?)";
                try (PreparedStatement subPstmt = conn.prepareStatement(subImgSql)) {
                    for (byte[] subBytes : subImagesBytes) {
                        String subImageFileName = ImageStorageManager.saveImage(subBytes);
                        if (subImageFileName != null) {
                            subPstmt.setInt(1, newAuctionId);
                            subPstmt.setString(2, subImageFileName);
                            subPstmt.addBatch(); 
                        }
                    }
                    subPstmt.executeBatch();
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
    public static synchronized Auction createAuction(CreateAuctionRequest request) {
        AuctionStatus parsedStatus;
        // Đặt mặc định làm WAITING nếu không có
        parsedStatus = (request.getStatus() != null)? request.getStatus() : AuctionStatus.WAITING; 
        if (parsedStatus == AuctionStatus.WAITING) {
            ServerLogger.info("Creating auction with WAITING status");
        } else { ServerLogger.info("Creating auction with ACTIVATED status"); }


        return createAuction(request.getSellerId(), request.getTitle(), 
                             request.getMainImageBytes(), request.getSubImagesBytes(),
                             request.getDescription(), 
                             request.getCategory(), request.getCategoryGroupedSpecs(), request.getStartingPrice(), 
                             request.getDuration(), request.getStartTime(), request.getEndTime(),
                             parsedStatus);
    }

    // Lấy các phiên đấu giá phục vụ cho tính năng xem lịch sử đâu giá
    // Lấy hết các phiên đấu giá kể cả đã kết thúc
    // ---------------------------------------------------------------
    public static List<Auction> getAuctions() {
        List<Auction> auctionList = new ArrayList<>();
        Map<Integer, Auction> auctionMap = new HashMap<>(); 
        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();
        
        String sql = "SELECT * FROM auctions";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while (rs.next()) {
                Auction auction = extractAuctionFromResultSet(rs, categoryMap);
                auctionList.add(auction);
                auctionMap.put(auction.getId(), auction); 
            }

            if (!auctionList.isEmpty()) {
                loadSubImagesForAuctions(auctionMap, conn);
                loadSpecificationsForAuctions(auctionMap, conn);
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
        Map<Integer, Auction> auctionMap = new HashMap<>(); 
        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();
        
        String sql = "SELECT * FROM auctions WHERE status = ?";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, status.name());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = extractAuctionFromResultSet(rs, categoryMap);
                    auctionList.add(auction);
                    auctionMap.put(auction.getId(), auction); 
                }
            }

            if (!auctionList.isEmpty()) {
                loadSubImagesForAuctions(auctionMap, conn);
                loadSpecificationsForAuctions(auctionMap, conn);
            }

        } catch (SQLException e) {
            ServerLogger.error("Database error getting auctions by status: " + e.getMessage());
        }
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
                    
                    // Since it's just one auction, we can put it in a map to reuse the batch methods
                    Map<Integer, Auction> map = new HashMap<>();
                    map.put(auction.getId(), auction);
                    
                    loadSubImagesForAuctions(map, conn);
                    loadSpecificationsForAuctions(map, conn);
                    
                    return auction;
                }
            }
        } catch (SQLException e) {
            ServerLogger.error("Error fetching auction by ID: " + e.getMessage());
        }
        return null;
    }

    // Lấy các hình ảnh của phiên đấu giá
    // ----------------------------------
    private static void loadSubImagesForAuctions(Map<Integer, Auction> auctionMap, Connection conn) throws SQLException {
        if (auctionMap.isEmpty()) return;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < auctionMap.size(); i++) {
            placeholders.append("?");
            if (i < auctionMap.size() - 1) placeholders.append(",");
        }

        String query = "SELECT auction_id, image_path FROM auction_images WHERE auction_id IN (" + placeholders + ")";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            int index = 1;
            for (Integer auctionId : auctionMap.keySet()) {
                pstmt.setInt(index++, auctionId);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int auctionId = rs.getInt("auction_id");
                    String imagePath = rs.getString("image_path");
                    
                    Auction targetAuction = auctionMap.get(auctionId);
                    if (targetAuction != null) {
                        targetAuction.getSubImagePaths().add(imagePath);
                    }
                }
            }
        }
    }

    
    // Lấy thông tin về các field người dùng đã nhập vào khi tạo phiên đấu giá
    // -----------------------------------------------------------------------
    private static void loadSpecificationsForAuctions(Map<Integer, Auction> auctionMap, Connection conn) throws SQLException {
        if (auctionMap.isEmpty()) return;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < auctionMap.size(); i++) {
            placeholders.append("?");
            if (i < auctionMap.size() - 1) placeholders.append(",");
        }

        String query = "SELECT auction_id, category_id, field_name, field_value FROM auction_specifications WHERE auction_id IN (" + placeholders + ")";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            int index = 1;
            for (Integer auctionId : auctionMap.keySet()) {
                pstmt.setInt(index++, auctionId);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int auctionId = rs.getInt("auction_id");
                    int catId = rs.getInt("category_id");
                    String name = rs.getString("field_name");
                    String value = rs.getString("field_value");
                    
                    Auction targetAuction = auctionMap.get(auctionId);
                    if (targetAuction != null) {
                        targetAuction.getCategoryGroupedSpecs()
                                     .computeIfAbsent(catId, k -> new HashMap<>())
                                     .put(name, value);
                    }
                }
            }
        }
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
            return false;
        }
    }
}