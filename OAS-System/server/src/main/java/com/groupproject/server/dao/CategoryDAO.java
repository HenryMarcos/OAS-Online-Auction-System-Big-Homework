package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;

public class CategoryDAO {

    public static Map<Integer, Category> getCategories() {
         ServerLogger.info("An user asked for categories");

        // Tìm tất cả hạng mục bằng id
        Map<Integer, Category> categoryMap = new HashMap<>();

        String sql = "SELECT c.id, c.name, c.parent_id, cf.field_name FROM categories c " +
                     "LEFT JOIN category_fields cf ON c.id = cf.category_id";

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            ServerLogger.info("Getting all the category");
            // Lấy tất cả các hàm và đưa chúng vào map
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                // Xử lý giá trị SQL NULL của parent_id
                Integer parentId = (rs.getObject("parent_id") != null)? 
                                    rs.getInt("parent_id") : null;

                Category category = categoryMap.computeIfAbsent(id, k -> new Category(id, name, parentId));

                // Thêm field vào category nếu nó chưa tồn tại
                String fieldName = rs.getString("field_name");
                if (fieldName != null) {
                    category.addRequiredField(fieldName);
                }
            }

            ServerLogger.info("Assigning fields to the categories");
        } catch (Exception e) {
            ServerLogger.error("Error fetching categories: " + e.getMessage());
        }

        ServerLogger.info("Finish getting categories");

        return categoryMap;
    }

    public static List<Category> getMainCategories() {
        ServerLogger.info("An user asked for main categories");
        // Danh sách này chỉ chứa những hạng mục chính
        List<Category> mainCategories = new ArrayList<>();
        Map<Integer, Category> categoryMap = getCategories();

        ServerLogger.info("Linking category child with it parent");
        // Link child với parent
        for (Category category : categoryMap.values()) {
            if (category.getParentId() == null) {
                // Nêu không có parent thì là một hạng mục chính
                mainCategories.add(category);
            } else {
                // Nếu có parent thì tìm parent và thêm vào hạng mục con
                Category parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    parent.addSubCategory(category);
                }
            }
        }

        ServerLogger.info("Successfully loaded and linked " + mainCategories.size() + " main categories.");

        return mainCategories;
    }

    public static List<Category> getMainCategories(Map<Integer, Category> categories) {
        ServerLogger.info("An user asked for main categories");
        // Danh sách này chỉ chứa những hạng mục chính
        List<Category> mainCategories = new ArrayList<>();

        ServerLogger.info("Linking category child with it parent");
        // Link child với parent
        for (Category category : categories.values()) {
            if (category.getParentId() == null) {
                // Nêu không có parent thì là một hạng mục chính
                mainCategories.add(category);
            } else {
                // Nếu có parent thì tìm parent và thêm vào hạng mục con
                Category parent = categories.get(category.getParentId());
                if (parent != null) {
                    parent.addSubCategory(category);
                }
            }
        }

        ServerLogger.info("Successfully loaded and linked " + mainCategories.size() + " main categories.");

        return mainCategories;
    }
}
