package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.request.PlaceBidRequest;

public class BidDAO {

    public static boolean insertBid(PlaceBidRequest request) {
        int bidderId = ClientContext.currentUser.get().getId();
        return insertBid(request.getAuctionId(), bidderId, request.getBidAmount());
    }

    public static boolean insertBid(int auctionId, int bidderId, double amount) {
        String checkSql = "SELECT a.status, a.current_bid, a.starting_price, u.balance " +
                          "FROM auctions a, users u WHERE a.id = ? AND u.id = ?";
        
        String insertBidSql = "INSERT INTO bids (auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        String updateAuctionSql = "UPDATE auctions SET current_bid = ?, current_bidder_id = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
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

    /**
     * Chuyển tiền từ người mua sang người bán (Đã dời từ AuctionManager sang)
     */
    public static boolean executeDirectTransfer(int winnerId, int sellerId, double amount) {
        String deductSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
        String addSql = "UPDATE users SET balance = balance + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
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