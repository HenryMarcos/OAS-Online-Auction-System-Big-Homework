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
                                "current_bidder INTEGER, " +
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

            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (1, 'Electronics', NULL);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (11, 'Laptops', 1);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'Brand');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'Model');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'RAM (GB)');");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (12, 'Smartphones', 1);");

            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (2, 'Clothing', NULL);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (21, 'Mens Shoes', 2);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (22, 'Womens Shirts', 2);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (22, 'Size');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (22, 'Color');");


            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (3, 'Home & Garden', NULL);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (31, 'Furniture', 3);");

            

            
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
