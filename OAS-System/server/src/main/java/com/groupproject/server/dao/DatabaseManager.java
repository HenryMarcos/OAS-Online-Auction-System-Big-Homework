package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.groupproject.server.utils.Config;
import com.groupproject.server.utils.ServerLogger;

public class DatabaseManager {

    private static DatabaseManager instance;

    private Connection connection;

    private DatabaseManager() {
        try {
            this.connection = DriverManager.getConnection(Config.DATABASE_URL);
            ServerLogger.info("Successfully connected to SQLite!");
        } catch (SQLException e) {
            ServerLogger.info("[FATAL ERROR] Could not connect to the database.");
            e.printStackTrace();
            // Optional: System.exit(1); // Kill the server if DB fails
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void initDatabse() {
        try (Statement stmt = connection.createStatement()) {

            // Tạo bảng users nếu chưa tồn tại 
            String sql = "CREATE TABLE IF NOT EXISTS users (" + 
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," + 
                         "username TEXT UNIQUE NOT NULL," + 
                         "email TEXT UNIQUE NOT NULL," +
                         "password TEXT NOT NULL)";
            stmt.execute(sql);
            
            String auctionSql = "CREATE TABLE IF NOT EXISTS auctions (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "seller_id INTEGER NOT NULL, " +
                                "title TEXT NOT NULL," +
                                "description TEXT NOT NULL," +
                                "category_id INTEGER NOT NULL," +
                                "starting_price REAL NOT NULL," +
                                "end_time DATETIME NOT NULL," +
                                "current_bid REAL NOT NULL," + 
                                "current_bidder_id INTEGER, " +
                                "status TEXT NOT NULL, " + 
                                "FOREIGN KEY(seller_id) REFERENCES users(id), " +
                                "FOREIGN KEY(category_id) REFERENCES categories(id))";
            stmt.execute(auctionSql);

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

            String specificationSql = "CREATE TABLE IF NOT EXISTS auction_specifications (" +
                                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                                      "auction_id INTEGER NOT NULL, " +
                                      "category_id INTEGER NOT NULL, " +
                                      "field_name TEXT NOT NULL, " +
                                      "field_value TEXT NOT NULL, " +
                                      "FOREIGN KEY(auction_id) REFERENCES auctions(id) ON DELETE CASCADE, " +
                                      "FOREIGN KEY(category_id) REFERENCES categories_(id))";
            stmt.execute(specificationSql);

            String itemsSql = "CREATE TABLE IF NOT EXISTS items ( " + 
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                                "title TEXT, " + 
                                "description TEXT, " + 
                                "starting_price REAL, " + 
                                "category_id INTEGER " + 
                                ");";
            stmt.execute(itemsSql);

            String itemAttributesSql = "CREATE TABLE IF NOT EXISTS item_attributes (" +
                                "auction_id INTEGER NOT NULL, " +
                                "field_name TEXT NOT NULL, " +
                                "field_value TEXT NOT NULL, " + // e.g., "Apple", "XL", "Red"
                                "FOREIGN KEY(auction_id) REFERENCES items(id))";
            stmt.execute(itemAttributesSql);

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

            

            
            CategoryDAO.getCategories();
            
            ServerLogger.info("Database initialized successfully!");
        } catch (Exception e) {
            ServerLogger.error(e.getMessage());
        }
    }

    // Phương thức helper để lấy kết nối kho dữ liệu (DRY principle)
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(Config.DATABASE_URL);
            }
        } catch (SQLException e) {
            ServerLogger.error(e.getMessage());
        }
        return connection;
    }
}
