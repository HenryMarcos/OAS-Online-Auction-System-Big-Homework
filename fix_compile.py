import os

am_path = 'OAS-System/server/src/main/java/com/groupproject/server/service/AuctionManager.java'
with open(am_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('AuctionFinishedEvent', 'AuctionFinisedEvent')

lines = content.split('\n')
new_lines = []
skip = False
for line in lines:
    if 'SystemNotificationEvent adminLog = new SystemNotificationEvent' in line:
        skip = True
        continue
    if skip:
        if ');' in line:
            skip = False
        continue
    if 'ClientManager.INSTANCE.broadcastToAdmins(adminLog);' in line:
        continue
    new_lines.append(line)

with open(am_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(new_lines))

# AuctionDAO
auction_dao_path = 'OAS-System/server/src/main/java/com/groupproject/server/dao/AuctionDAO.java'
with open(auction_dao_path, 'r', encoding='utf-8') as f:
    dao_content = f.read()
    
dao_content = dao_content.rstrip()
if dao_content.endswith('}'):
    dao_content = dao_content[:-1]

missing_methods = """
    public static boolean updateAuctionStatusOnly(int auctionId, com.groupproject.shared.model.enums.AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            com.groupproject.server.utils.ServerLogger.error("AuctionDAO:updateAuctionStatusOnly: " + e.getMessage());
            return false;
        }
    }
    
    public static java.util.List<Integer> getExpiredWaitingAuctions(java.time.LocalDateTime time) {
        return new java.util.ArrayList<>();
    }
    
    public static java.util.List<Integer> getMissedScheduledAuctions(java.time.LocalDateTime time) {
        return new java.util.ArrayList<>();
    }
    
    public static java.util.List<Integer> getExpiredActiveAuctions(java.time.LocalDateTime time) {
        return new java.util.ArrayList<>();
    }
}
"""
with open(auction_dao_path, 'w', encoding='utf-8') as f:
    f.write(dao_content + missing_methods)

# BidDAO
bid_dao_path = 'OAS-System/server/src/main/java/com/groupproject/server/dao/BidDAO.java'
with open(bid_dao_path, 'r', encoding='utf-8') as f:
    bid_content = f.read()

bid_content = bid_content.rstrip()
if bid_content.endswith('}'):
    bid_content = bid_content[:-1]

missing_bid = """
    public static boolean executeDirectTransfer(int buyerId, int sellerId, double amount) {
        java.sql.Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            String checkSql = "SELECT balance FROM users WHERE id = ?";
            try (java.sql.PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, buyerId);
                java.sql.ResultSet rs = checkStmt.executeQuery();
                if (!rs.next() || rs.getDouble("balance") < amount) {
                    conn.rollback();
                    return false;
                }
            }
            
            String deductSql = "UPDATE users SET balance = balance - ? WHERE id = ?";
            try (java.sql.PreparedStatement deductStmt = conn.prepareStatement(deductSql)) {
                deductStmt.setDouble(1, amount);
                deductStmt.setInt(2, buyerId);
                deductStmt.executeUpdate();
            }
            
            String addSql = "UPDATE users SET balance = balance + ? WHERE id = ?";
            try (java.sql.PreparedStatement addStmt = conn.prepareStatement(addSql)) {
                addStmt.setDouble(1, amount);
                addStmt.setInt(2, sellerId);
                addStmt.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (java.sql.SQLException e) {
            if (conn != null) try { conn.rollback(); } catch(java.sql.SQLException ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch(java.sql.SQLException ex) {}
        }
    }
    
    public static java.util.List<Integer> getUniqueBidders(int auctionId) {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT user_id FROM bids WHERE auction_id = ?";
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            java.sql.ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getInt("user_id"));
            }
        } catch (java.sql.SQLException e) {}
        return result;
    }
}
"""
with open(bid_dao_path, 'w', encoding='utf-8') as f:
    f.write(bid_content + missing_bid)
