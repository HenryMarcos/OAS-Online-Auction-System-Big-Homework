package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.BidDTO;
import com.groupproject.shared.network.requests.PlaceBidRequest;

public class BidDAO {

    // Returns true if successful, false if something failed
    public static boolean insertBid(int auctionId, int bidderId, double amount) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, LocalDateTime.now().toString());
            
            int rowsAffected = pstmt.executeUpdate();
            
            // Also, update the current_price in the auctions table!
            if (rowsAffected > 0) {
                updateAuctionPrice(conn, auctionId, amount);
                return true;
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to insert bid: " + e.getMessage());
        }
        return false;
    }

    public static boolean insertBid(PlaceBidRequest request, ClientHandler clientContext) {
        return insertBid(request.getAuctionId(), clientContext.getAuthenticatedUserId(), request.getBidAmount());
    }

    private static void updateAuctionPrice(Connection conn, int auctionId, double newPrice) throws Exception {
        String sql = "UPDATE auctions SET current_bid = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, auctionId);
            pstmt.executeUpdate();
        }
    }

    public static boolean insertBid2(int auctionId, int bidderId, double amount) {
        String checkSql = "SELECT a.status, a.current_bid, a.starting_price, u.balance " +
                          "FROM auctions a, users u WHERE a.id = ? AND u.id = ? FOR UPDATE";
        
        String insertBidSql = "INSERT INTO bids (auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        String updateAuctionSql = "UPDATE auctions SET current_bid = ?, current_bidder_id = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu Transaction an toàn

            try {
                // 1. Kiểm tra toàn bộ điều kiện (Status, Price, Balance)
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, auctionId);
                    checkStmt.setInt(2, bidderId);
                    
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            String status = rs.getString("status");
                            double currentBid = rs.getDouble("current_bid");
                            double startingPrice = rs.getDouble("starting_price");
                            double balance = rs.getDouble("balance");

                            // Kiểm tra Status
                            if (!"ACTIVED".equalsIgnoreCase(status)) {
                                ServerLogger.warning("Bid rejected: Auction not active.");
                                conn.rollback(); return false;
                            }

                            // Kiểm tra Giá
                            if (currentBid == 0.0) {
                                if (amount < startingPrice) { // Lần đầu bid phải >= starting_price
                                    ServerLogger.warning("Bid rejected: Amount < starting price.");
                                    conn.rollback(); return false;
                                }
                            } else {
                                if (amount <= currentBid) { // Đã có người bid thì phải > current_bid
                                    ServerLogger.warning("Bid rejected: Amount <= current bid.");
                                    conn.rollback(); return false;
                                }
                            }

                            // Kiểm tra Số dư
                            if (balance < amount) {
                                ServerLogger.warning("Bid rejected: Insufficient balance.");
                                conn.rollback(); return false;
                            }
                        } else {
                            ServerLogger.warning("Bid rejected: Auction or User not found.");
                            conn.rollback(); return false;
                        }
                    }
                }

                // 2. Thêm lịch sử đặt giá vào bảng bids
                try (PreparedStatement insertStmt = conn.prepareStatement(insertBidSql)) {
                    insertStmt.setInt(1, auctionId);
                    insertStmt.setInt(2, bidderId);
                    insertStmt.setDouble(3, amount);
                    insertStmt.executeUpdate();
                }

                // 3. Cập nhật bảng auctions (giá mới nhất và người giữ giá)
                try (PreparedStatement updateStmt = conn.prepareStatement(updateAuctionSql)) {
                    updateStmt.setDouble(1, amount);
                    updateStmt.setInt(2, bidderId);
                    updateStmt.setInt(3, auctionId);
                    updateStmt.executeUpdate();
                }

                conn.commit(); // Hoàn tất Transaction
                return true;

            } catch (SQLException e) {
                conn.rollback();
                ServerLogger.error("Transaction failed, rolling back. Error: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            ServerLogger.error("Database connection error: " + e.getMessage());
            return false;
        }
    }

    public static List<BidDTO> getBidsForAuction(int auctionId) {
        List<BidDTO> pastBids = new ArrayList<>();
        // ASC ensures oldest bids are first (perfect for graph drawing)
        String sql = "SELECT bidder_id, amount, bid_time FROM bids WHERE auction_id = ? ORDER BY bid_time ASC";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int bidderId = rs.getInt("bidder_id");
                    double amount = rs.getDouble("amount");
                    String timeStr = rs.getString("bid_time");
                    
                    LocalDateTime bidTime = timeStr != null ? LocalDateTime.parse(timeStr) : LocalDateTime.now();
                    pastBids.add(new BidDTO("User " + bidderId, amount, bidTime));
                }
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to fetch historical bids: " + e.getMessage());
        }
        return pastBids;
    }

    public static List<Integer> getUniqueBidders(int auctionId) {
        List<Integer> bidderIds = new ArrayList<>();
        String sql = "SELECT DISTINCT bidder_id FROM bids WHERE auction_id = ?";
        
        try (java.sql.Connection conn = DatabaseManager.INSTANCE.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bidderIds.add(rs.getInt("bidder_id"));
                }
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to fetch unique bidders: " + e.getMessage());
        }
        return bidderIds;
    }

    /**
     * Chuyển tiền từ người mua sang người bán (Đã dời từ AuctionManager sang)
     */
    public static boolean executeDirectTransfer(int winnerId, int sellerId, double amount) {
        String deductSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
        String addSql = "UPDATE users SET balance = balance + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deduct = conn.prepareStatement(deductSql);
                 PreparedStatement add = conn.prepareStatement(addSql)) {
                
                deduct.setDouble(1, amount);
                deduct.setInt(2, winnerId);
                deduct.executeUpdate();

                add.setDouble(1, amount);
                add.setInt(2, sellerId);
                add.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            ServerLogger.error("Transfer error: " + e.getMessage());
            return false;
        }
    }
}
