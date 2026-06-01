import re

with open('server/src/main/java/com/groupproject/server/dao/BidDAO.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix DatabaseManager
content = content.replace('DatabaseManager.getInstance().getConnection()', 'DatabaseManager.INSTANCE.getConnection()')
content = content.replace('DatabaseManager.getConnection()', 'DatabaseManager.INSTANCE.getConnection()')

# Add SQLException import
if 'import java.sql.SQLException;' not in content:
    content = content.replace('import java.sql.ResultSet;', 'import java.sql.ResultSet;\nimport java.sql.SQLException;')

# Add executeDirectTransfer if missing
if 'executeDirectTransfer' not in content:
    content = content[:content.rindex('}')] + '''
    public static boolean executeDirectTransfer(int buyerId, int sellerId, double amount) {
        java.sql.Connection conn = null;
        try {
            conn = DatabaseManager.INSTANCE.getConnection();
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
}
'''

with open('server/src/main/java/com/groupproject/server/dao/BidDAO.java', 'w', encoding='utf-8') as f:
    f.write(content)

with open('server/src/main/java/com/groupproject/server/service/AuctionManager.java', 'r', encoding='utf-8') as f:
    am_content = f.read()

# Fix AuctionFinisedEvent constructor
am_content = am_content.replace('AuctionFinisedEvent finishedEvent = new AuctionFinisedEvent(auctionId);', 'AuctionFinisedEvent finishedEvent = new AuctionFinisedEvent(auctionId, auction.getHighestBidderId() != null ? auction.getHighestBidderId() : 0, auction.getCurrentBid());')

with open('server/src/main/java/com/groupproject/server/service/AuctionManager.java', 'w', encoding='utf-8') as f:
    f.write(am_content)
