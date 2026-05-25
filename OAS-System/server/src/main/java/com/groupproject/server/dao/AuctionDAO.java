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
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.CreateAuctionRequest;

public class AuctionDAO {
    // Tạo phiên đấu giá mới
    // ---------------------
    public static synchronized Auction createAuction(int sellerId, String title, String description, Category category, 
                                                     Map<Integer, Map<String, String>> categoryGroupedSpecs , 
                                                     double startingPrice, LocalDateTime endTime, AuctionStatus status) {
        ServerLogger.info("Creating new auction");

        String auctionSql = "INSERT INTO auctions (seller_id, title, description, category_id, starting_price, end_time, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        String specSql = "INSERT INTO auction_specifications (auction_id, category_id, field_name, field_value) " +
                                  "VALUES (?, ?, ?, ?)";
        

        
        boolean originalAutoCommit = true;

        try (Connection conn = DatabaseManager.INSTANCE.getConnection(); ) {

            if (conn == null) {
                ServerLogger.error("Could not obtain a database connection from the pool.");
                return null;
            }

            // Bắt đầu giao dịch để duy trì tính toàn vẹn của cơ sở dữ liệu
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(auctionSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement specPstmt = conn.prepareStatement(specSql)) {
                pstmt.setInt(1, sellerId);
                pstmt.setString(2, title);
                pstmt.setString(3, description);
                pstmt.setInt(4, category.getId());
                pstmt.setDouble(5, startingPrice);
                pstmt.setString(6, endTime.toString());
                pstmt.setString(7, status.name());

                ServerLogger.info("Prepare to execute prepared statement");
                pstmt.executeUpdate();


                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        ServerLogger.info("Successfully executed prepared statement");
                        int newAuctionId = rs.getInt(1);

                        // If dynamic attributes exist, queue them up as a batch insert
                        if (categoryGroupedSpecs != null && !categoryGroupedSpecs.isEmpty()) {
                            for (Map.Entry<Integer, Map<String, String>> categoryEntry : categoryGroupedSpecs.entrySet()) {
                                int specCategoryId = categoryEntry.getKey();
                                Map<String, String> fields = categoryEntry.getValue();

                                for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
                                    specPstmt.setInt(1, newAuctionId);
                                    specPstmt.setInt(2, specCategoryId);
                                    specPstmt.setString(3, fieldEntry.getKey());
                                    specPstmt.setString(4, fieldEntry.getValue());
                                    specPstmt.addBatch();
                                }
                            }
                            specPstmt.executeBatch();
                        }

                        // Commit entire batch together if no errors occurred
                        conn.commit();
                        ServerLogger.info("Successfully created auction ID: " + newAuctionId);
                        
                        return new Auction(newAuctionId, sellerId, title, description, category, categoryGroupedSpecs, startingPrice, endTime, status);
                    } else {
                        ServerLogger.error("Failed to execute prepared statement");
                    }
                }
            } catch (SQLException transactionEx) {
                // This catch block is INSIDE the outer try, meaning 'conn' is alive and fully accessible!
                try {
                    ServerLogger.error("Auction insertion failed. Rolling back transaction... Error: " + transactionEx.getMessage());
                    conn.rollback(); // Rollback changes safely
                } catch (SQLException rollbackEx) {
                    ServerLogger.error("Critical error during transaction rollback: " + rollbackEx.getMessage());
                }
            } finally {
                // Restore connection auto-commit rules before handing it back to the HikariCP pool
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    ServerLogger.error("Failed to restore connection auto-commit state: " + e.getMessage());
                }
            }
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


        return createAuction(request.getSellerId(), request.getTitle(), request.getDescription(), 
                             request.getCategory(), request.getCategoryGroupedSpecs(), 
                             request.getStartingPrice(), LocalDateTime.parse(request.getEndTime()),
                             parsedStatus);
    }

    // Lấy các phiên đấu giá phục vụ cho tính năng xem lịch sử đâu giá
    // Lấy hết các phiên đấu giá kể cả đã kết thúc
    // ---------------------------------------------------------------
    public static List<Auction> getAuctions() {
        List<Auction> auctionList = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Lấy các thông tin cơ bản của phiên đấu giá
                // ------------------------------------------
                int id = rs.getInt("id");
                int sellerId = rs.getInt("seller_id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                int categoryId = rs.getInt("category_id");
                double startingPrice = rs.getDouble("starting_price");
                String endTimeStr = rs.getString("end_time");
                double currentBid = rs.getDouble("current_bid");
                Integer currentBidderId;
                String status = rs.getString("status");

                // Lấy id của người đấu giá(đảm bảo an toàn nếu null)
                int bidderIdRaw = rs.getInt("current_bidder_id");
                currentBidderId = rs.wasNull()? null : bidderIdRaw;

                LocalDateTime endTime = (endTimeStr != null)? LocalDateTime.parse(endTimeStr) : null;

                Category category = categoryMap.get(categoryId);

                Map<Integer, Map<String, String>> specs = getAuctionSpecifications(id, conn);

                // Tạo lớp Auction từ các thông tin lấy được phía trên
                // ---------------------------------------------------
                Auction auction = new Auction(id, sellerId, title, description, category, specs, startingPrice, endTime);
                auction.setCurrentBid(currentBid);
                auction.setHighestBidderId(currentBidderId);
                auction.setStatus(AuctionStatus.valueOf(status.toUpperCase()));

                auctionList.add(auction);
            }
            
        } catch (SQLException e) {
            ServerLogger.error("Database error getting auction: " + e.getMessage());
        }

        return auctionList;
    }

    // Lấy các phiên đấu giá theo trạng thái của chúng
    // -----------------------------------------------
    public static List<Auction> getAuctionsByStatus(AuctionStatus status) {
        ServerLogger.info("Getting auctions with status " + status.name());
        List<Auction> auctionList = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ?";
        Map<Integer, Category> categoryMap = CategoryManager.INSTANCE.getCategories();

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            
            pstmt.setString(1, status.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Lấy các thông tin cơ bản của phiên đấu giá
                    // ------------------------------------------
                    int id = rs.getInt("id");
                    int sellerId = rs.getInt("seller_id");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    int categoryId = rs.getInt("category_id");
                    double startingPrice = rs.getDouble("starting_price");
                    String startTimeStr = rs.getString("start_time");
                    String endTimeStr = rs.getString("end_time");
                    double currentBid = rs.getDouble("current_bid");

                    // Lấy id của người đấu giá (đảm bảo an toàn nếu null)
                    Integer currentBidderId = (Integer) rs.getObject("current_bidder_id");

                    LocalDateTime startTime = (startTimeStr != null)? LocalDateTime.parse(startTimeStr) : null;
                    LocalDateTime endTime = (endTimeStr != null)? LocalDateTime.parse(endTimeStr) : null;

                    Category category = categoryMap.get(categoryId);

                    Map<Integer, Map<String, String>> specs = getAuctionSpecifications(id, conn);

                    Auction auction = new Auction(id, sellerId, title, description, category, specs, startingPrice, endTime, status);
                    auction.setCurrentBid(currentBid);
                    auction.setHighestBidderId(currentBidderId);

                    auctionList.add(auction);
                }
            }
        } catch (Exception e) {
            ServerLogger.error("Database error getting " + status.name() +" auctions: " + e.getMessage());
            return null;
        }
        if (auctionList == null || auctionList.isEmpty()) {
            ServerLogger.warning("Got 0 auctions");
        } else {
            ServerLogger.info("Successfully get " + status.name() + " auctions, found " + auctionList.size());
        }

        return auctionList;
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
    
    // Lấy thông tin về các field người dùng đã nhập vào khi tạo phiên đấu giá
    // -----------------------------------------------------------------------
    private static Map<Integer, Map<String, String>> getAuctionSpecifications(int auctionId, Connection conn) throws SQLException {
        Map<Integer, Map<String, String>> groupedSpecs = new HashMap<>();
        String query = "SELECT category_id, field_name, field_value FROM auction_specifications WHERE auction_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int catId = rs.getInt("category_id");
                    String name = rs.getString("field_name");
                    String value = rs.getString("field_value");

                    groupedSpecs.computeIfAbsent(catId, k -> new HashMap<>()).put(name, value);
                }
            }
        } 

        return groupedSpecs;
    }

}