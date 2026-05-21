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

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.CreateAuctionRequest;

public class AuctionDAO {
    public static synchronized Auction createAuction(int sellerId, String title, String description, Category category, 
                                                     Map<Integer, Map<String, String>> categoryGroupedSpecs , 
                                                     double startingPrice, LocalDateTime endTime) {
        ServerLogger.info("Creating new auction");

        String auctionSql = "INSERT INTO auctions (seller_id, title, description, category_id, starting_price, end_time, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        String specSql = "INSERT INTO auction_specifications (auction_id, category_id, field_name, field_value) " +
                                  "VALUES (?, ?, ?, ?)";
        

        Connection conn = DatabaseManager.getInstance().getConnection();
        boolean originalAutoCommit = true;

        try {
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
                pstmt.setString(7, "WAITING");

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
                        
                        return new Auction(newAuctionId, sellerId, title, description, category, categoryGroupedSpecs, startingPrice, endTime);
                    } else {
                        ServerLogger.error("Failed to execute prepared statement");
                    }
                }
            }
        } catch (SQLException e) {
            try {
                conn.rollback(); // Rollback changes if anything failed
                ServerLogger.error("Auction insertion failed. Transaction rolled back.");
            } catch (SQLException rollbackEx) {
                ServerLogger.error("Critical error during transaction rollback: " + rollbackEx.getMessage());
            }
            ServerLogger.error("Database error creating auction: " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit); // Restore database connection rules
            } catch (SQLException e) {
                ServerLogger.error("Failed to restore connection auto-commit state: " + e.getMessage());
            }
        }
        return null;
    }

    public static synchronized Auction createAuction(CreateAuctionRequest request) {
        return createAuction(request.getSellerId(), request.getTitle(), request.getDescription(), 
                             request.getCategory(), request.getCategoryGroupedSpecs(), 
                             request.getStartingPrice(), LocalDateTime.parse(request.getEndTime()));
    }

    // Lấy các phiên đấu giá phục vụ cho tính năng xem lịch sử đâu giá
    // Lấy hết các phiên đấu giá kể cả đã kết thúc
    public static List<Auction> getAuctions() {
        List<Auction> auctionList = new ArrayList<>();
        String sql = "SELECT * FROM auctions";
        Connection conn = DatabaseManager.getInstance().getConnection();

        Map<Integer, Category> categoryMap = CategoryDAO.getCategories();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Lấy các thông tin cơ bản của phiên đấu giá
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

    // Chỉ lấy những phiên đấu giá đang hoạt động (WAITING hoặc ACTIVED) 
    // và thời gian kết thúc phải lớn hơn thời gian hiện tại
    public static List<Auction> getActiveAuctions() {
        List<Auction> activedAuctionList = new ArrayList<>();
        
        // SQL: Chỉ lấy những phiên đang WAITING hoặc ACTIVED và thời gian kết thúc phải lớn hơn thời gian hiện tại
        String sql = "SELECT * FROM auctions WHERE status IN ('WAITING', 'ACTIVED') AND end_time > ?";
        
        Map<Integer, Category> categoryMap = CategoryDAO.getCategories();

        // Sử dụng try-with-resources để tự động đóng Connection, PreparedStatement và ResultSet
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            // Truyền thời gian hiện tại của Server (dạng chuỗi) vào để Database lọc giúp
            pstmt.setString(1, LocalDateTime.now().toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int sellerId = rs.getInt("seller_id");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    int categoryId = rs.getInt("category_id");
                    double startingPrice = rs.getDouble("starting_price");
                    String endTimeStr = rs.getString("end_time");
                    double currentBid = rs.getDouble("current_bid");
                    String status = rs.getString("status");

                    // Lấy id của người đấu giá (đảm bảo an toàn nếu null)
                    int bidderIdRaw = rs.getInt("current_bidder_id");
                    Integer currentBidderId = rs.wasNull() ? null : bidderIdRaw;

                    LocalDateTime endTime = (endTimeStr != null) ? LocalDateTime.parse(endTimeStr) : null;
                    Category category = categoryMap.get(categoryId);

                    Map<Integer, Map<String, String>> specs = getAuctionSpecifications(id, conn);

                    Auction auction = new Auction(id, sellerId, title, description, category, specs, startingPrice, endTime);
                    auction.setCurrentBid(currentBid);
                    auction.setHighestBidderId(currentBidderId);
                    auction.setStatus(AuctionStatus.valueOf(status.toUpperCase()));

                    activedAuctionList.add(auction);
                }
            }
        } catch (SQLException e) {
            ServerLogger.error("Database error getting active auctions: " + e.getMessage());
        }

        return activedAuctionList;
    }

    
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