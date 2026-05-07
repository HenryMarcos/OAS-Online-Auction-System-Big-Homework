package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import com.groupproject.server.utils.Config;

public class DatabaseManager {
    public static void initDatabse() {
        try (Connection conn = DriverManager.getConnection(Config.DATABASE_URL); 
             Statement stmt = conn.createStatement()) {

            // Tạo bảng users nếu chưa tồn tại 
            String sql = "CREATE TABLE IF NOT EXISTS users (" + 
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," + 
                         "username TEXT UNIQUE NOT NULL," + 
                         "email TEXT UNIQUE NOT NULL," +
                         "password TEXT NOT NULL)";
            stmt.execute(sql);
            
            String auctionSql = "CREATE TABLE IF NOT EXISTS auctions (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "item_name TEXT NOT NULL," +
                                "starting_price REAL NOT NULL," +
                                "auction_duration LONG NOT NULL," +
                                "current_bid REAL NOT NULL," +
                                "highest_bidder TEXT," +
                                "end_time DATETIME NOT NULL," + 
                                "is_active BOOLEAN DEFAULT 1)";
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
            
            System.out.println("Database initialized successfully!");
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}
