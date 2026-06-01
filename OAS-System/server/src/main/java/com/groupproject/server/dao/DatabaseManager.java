package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.groupproject.server.utils.Config;
import com.groupproject.server.utils.ServerLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public enum DatabaseManager {
    INSTANCE;

    private HikariDataSource dataSource;

    private DatabaseManager() {
        try {
            // Config nhóm kết nối
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(Config.DATABASE_URL);

            // Setting của pool
            hikariConfig.setMaximumPoolSize(10); // Hỗ trợ tối đa 10 kết nối đồng thời
            hikariConfig.setMinimumIdle(2); // Luôn giữ ít nhất 2 kết nối đang mở và chờ 
            hikariConfig.setConnectionTimeout(30000); // Chờ khoảng 30s trước khi bỏ cuộc vì pool bị đầy

            // Các setting cụ thể để giúp SQLite hoạt động tốt với xử lý đồng thời
            hikariConfig.addDataSourceProperty("journal_mode", "WAL"); // Ghi log trước khi ghi để cải thiện khả năng xử lý đồng thời
            hikariConfig.addDataSourceProperty("busy_timeout", "5000");

            // Khởi tạo pool
            this.dataSource = new HikariDataSource(hikariConfig);

            ServerLogger.info("HikariCP Connection Pool successfully initialized!");
        } catch (Exception e) {
            ServerLogger.info("[FATAL ERROR] Could not connect to the database: " + e.getMessage());
            // Optional: System.exit(1); // Kill the server if DB fails
        }
    }

    public void initDatabse() {
        try (Statement stmt = getConnection().createStatement()) {

            // Tạo bảng users nếu chưa tồn tại 
            String sql = "CREATE TABLE IF NOT EXISTS users (" + 
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," + 
                         "username TEXT UNIQUE NOT NULL," + 
                         "email TEXT UNIQUE NOT NULL," +
                         "password TEXT NOT NULL," +
                         "balance REAL DEFAULT 10000," +
                         "created_at DATETIME NOT NULL)";
            stmt.execute(sql);

            // Tạo bảng chứa các thông báo
            String notificationsSql = "CREATE TABLE notifications (" + //
                                      "    id INTEGER PRIMARY KEY AUTOINCREMENT," + //
                                      "    user_id INTEGER NOT NULL," + //
                                      "    message TEXT NOT NULL," + //
                                      "    is_read BOOLEAN DEFAULT 0," + //
                                      "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " + //
                                      "    FOREIGN KEY(user_id) REFERENCES users(id) " +
                                      ");";
            stmt.execute(notificationsSql);
            

            // Tao bảng admin_list để lưu danh sách các user có quyền admin (chỉ chứa id của user, liên kết với bảng users)
            String sqlAdmin = "CREATE TABLE IF NOT EXISTS admin_list (" +
                              "user_id INTEGER PRIMARY KEY," +
                            "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)";
            stmt.execute(sqlAdmin);

            // Tạo bảng chứa các phiên đấu giá
            String auctionSql = "CREATE TABLE IF NOT EXISTS auctions (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "seller_id INTEGER NOT NULL, " +
                                "title TEXT NOT NULL," +
                                "main_image_path TEXT, " +
                                "description TEXT NOT NULL," +
                                "category_id INTEGER NOT NULL," +
                                "starting_price REAL NOT NULL," +
                                "duration INT DEFAULT 0, " +
                                "start_time DATETIME, " +
                                "end_time DATETIME NOT NULL," +
                                "current_bid REAL," +
                                "current_bidder_id INTEGER, " +
                                "status TEXT NOT NULL, " +
                                "FOREIGN KEY(seller_id) REFERENCES users(id), " +
                                "FOREIGN KEY(current_bidder_id) REFERENCES users(id), " +
                                "FOREIGN KEY(category_id) REFERENCES categories(id))";
            stmt.execute(auctionSql);

            String auctionImagesSql = "CREATE TABLE IF NOT EXISTS auction_images (" +
                                      "id INTEGER AUTO_INCREMENT PRIMARY KEY, " +
                                      "auction_id INTEGER NOT NULL, " +
                                      "image_path TEXT NOT NULL, " +
                                      "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE)";
            stmt.execute(auctionImagesSql);

            // Xóa trước khi tạo bảng để test(sau này sẽ không dùng)
            stmt.execute("DROP TABLE IF EXISTS category_fields");
            stmt.execute("DROP TABLE IF EXISTS categories");

            // Tạo bảng danh sách các danh mục hàng
            String categoriesSql = "CREATE TABLE IF NOT EXISTS categories (" +
                                   "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                   "name TEXT NOT NULL, " +
                                   "parent_id INTEGER, " +
                                   "FOREIGN KEY(parent_id) REFERENCES categories(id))";
            stmt.execute(categoriesSql);

            // Tạo bảng chứa các yêu cầu trong danh mục hàng
            String fieldsSql = "CREATE TABLE IF NOT EXISTS category_fields (" +
                               "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                               "category_id INTEGER NOT NULL, " +
                               "field_name TEXT NOT NULL, " +
                               "FOREIGN KEY(category_id) REFERENCES categories(id))";
            stmt.execute(fieldsSql);

            // Tạo bảng chứa các thông tin cần thiết của các danh mục hàng
            String specificationSql = "CREATE TABLE IF NOT EXISTS auction_specifications (" +
                                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                                      "auction_id INTEGER NOT NULL, " +
                                      "category_id INTEGER NOT NULL, " +
                                      "field_name TEXT NOT NULL, " +
                                      "field_value TEXT NOT NULL, " +
                                      "FOREIGN KEY(auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                                      "FOREIGN KEY(category_id) REFERENCES categories(id))";
            stmt.execute(specificationSql);

            // Tạo bảng chứa lịch sử các bid của các phiên đấu giá
            String bidsSql = "CREATE TABLE IF NOT EXISTS bids (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                             "auction_id INTEGER NOT NULL, " +
                             "bidder_id INTEGER NOT NULL, " +
                             "amount REAL NOT NULL, " +
                             "bid_time DATETIME NOT NULL, " +
                             "FOREIGN KEY(auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                             "FOREIGN KEY(bidder_id) REFERENCES users(id))";
            stmt.execute(bidsSql);

            // =====================================================================
            // 1. ELECTRONICS CATEGORY TREE
            // =====================================================================

            // --- Electronics (Parent) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (1, 'Electronics', NULL);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (1, 'Condition');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (1, 'Brand');");

            // --- Laptops & Computers (Subcategory) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (11, 'Laptops & Computers', 1);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'Processor');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'RAM (GB)');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'Storage Capacity');");

            // --- Cell Phones & Smartphones (Subcategory) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (12, 'Cell Phones & Smartphones', 1);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (12, 'Model');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (12, 'Color');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (12, 'Network / Carrier');");


            // =====================================================================
            // 2. MOTORS & VEHICLES CATEGORY TREE
            // =====================================================================

            // --- Motors (Parent) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (2, 'Motors', NULL);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (2, 'Make');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (2, 'Year');");

            // --- Cars & Trucks (Subcategory) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (21, 'Cars & Trucks', 2);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (21, 'Model');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (21, 'Mileage');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (21, 'Transmission');");

            // --- Motorcycles (Subcategory) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (22, 'Motorcycles', 2);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (22, 'Engine Size (cc)');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (22, 'Type (Sport/Cruiser)');");


            // =====================================================================
            // 3. FASHION & CLOTHING CATEGORY TREE (3 Levels Deep)
            // =====================================================================

            // --- Clothing & Accessories (Parent) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (3, 'Clothing & Accessories', NULL);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (3, 'Condition');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (3, 'Brand');");

            // --- Men's Clothing (Subcategory Level 1) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (31, 'Mens Clothing', 3);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (31, 'Size Type (Regular/Tall)');");

            // --- Men's Shoes (Subcategory Level 2) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (311, 'Mens Shoes', 31);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (311, 'US Shoe Size');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (311, 'Color');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (311, 'Style');");

            // --- Women's Clothing (Subcategory Level 1) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (32, 'Womens Clothing', 3);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (32, 'Size Type (Regular/Petite)');");


            // =====================================================================
            // 4. COLLECTIBLES & ART CATEGORY TREE
            // =====================================================================

            // --- Collectibles (Parent) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (4, 'Collectibles', NULL);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (4, 'Original / Reproduction');");

            // --- Sports Trading Cards (Subcategory) + Required Fields ---
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (41, 'Sports Trading Cards', 4);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (41, 'Sport');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (41, 'Player / Athlete');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (41, 'Graded (Yes/No)');");
            
            CategoryDAO.getMainCategories();
            
            ServerLogger.info("Database initialized successfully!");

            // --- SEED DATA: TẠO ADMIN ĐỂ TEST ---
            try {
                // 1. Tạo user 'admin' vào bảng users (nếu chưa tồn tại)
                // Mình dùng ID 999999 cho dễ nhớ
                String seedUser = "INSERT OR IGNORE INTO users (id, username, email, password, balance, created_at) " +
                                "VALUES (999999, 'admin', 'admin@test.com', 'admin123', 999999.0, '" + java.time.LocalDateTime.now() + "')";
                stmt.execute(seedUser);

                // 2. Thêm ID 999999 vào bảng admin_list để xác nhận quyền Admin
                String seedAdmin = "INSERT OR IGNORE INTO admin_list (user_id) VALUES (999999)";
                stmt.execute(seedAdmin);
                
                ServerLogger.info(">>> Seed Data: Tài khoản admin/admin123 đã sẵn sàng.");
            } catch (Exception e) {
                ServerLogger.error("Lỗi khi tạo dữ liệu mẫu: " + e.getMessage());
            }
        } catch (Exception e) {
            ServerLogger.error(e.getMessage());
        }
    }

    // Phương thức helper để lấy kết nối kho dữ liệu (DRY principle)
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            ServerLogger.error("Failed to get connection from Hikari pool: " + e.getMessage());
            return null;
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
