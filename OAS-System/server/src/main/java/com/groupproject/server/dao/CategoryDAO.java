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

        String categoriesSql = "SELECT id, name, parent_id FROM categories";
        String fieldsSql = "SELECT id, category_id, field_name FROM category_fields";
        Connection conn = DatabaseManager.getInstance().getConnection();

        try (PreparedStatement categoriesPstmt = conn.prepareStatement(categoriesSql);
             ResultSet categoriesRs = categoriesPstmt.executeQuery();
             PreparedStatement fieldsPstmt = conn.prepareStatement(fieldsSql);
             ResultSet fieldsRs = fieldsPstmt.executeQuery()) {
            
            ServerLogger.info("Getting all the category");
            // Lấy tất cả các hàm và đưa chúng vào map
            while (categoriesRs.next()) {
                int id = categoriesRs.getInt("id");
                String name = categoriesRs.getString("name");

                // Xử lý giá trị SQL NULL của parent_id
                Integer parentId = null;
                if (categoriesRs.getObject("parent_id") != null) {
                    parentId = categoriesRs.getInt("parent_id");
                }

                Category category = new Category(id, name, parentId);
                categoryMap.put(id, category);
            }

            ServerLogger.info("Assigning fields to the categories");
            // Thêm các field vào category
            while (fieldsRs.next()) {
                int id = fieldsRs.getInt("id");
                int category_id = fieldsRs.getInt("category_id");
                String field_name = fieldsRs.getString("field_name");

                Category category = categoryMap.get(category_id);
                if (category != null) {
                    category.addRequiredField(field_name);
                }
            }
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

        ServerLogger.info("Getting main categories finished, displaying the result:");

        for (Category category : mainCategories) {
            category.print("");
        }

        return mainCategories;
    }

    // The method to fetch the required fields for a specific category
    public static List<String> getRequiredFieldsForCategory(int categoryId) {
        List<String> fields = new ArrayList<>();
        
        // The SQL query to get just the field names for this exact category
        String sql = "SELECT field_name FROM category_fields WHERE category_id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();

        // Try-with-resources automatically closes the database connection when done
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Replace the '?' in the SQL string with the actual categoryId
            pstmt.setInt(1, categoryId);
            
            // Execute the query
            ResultSet rs = pstmt.executeQuery();
            
            // Loop through the results and add them to our list
            while (rs.next()) {
                fields.add(rs.getString("field_name"));
            }
            
        } catch (Exception e) {
            ServerLogger.error("Error fetching category fields: " + e.getMessage());
        }
        
        return fields; // Returns something like ["Brand", "RAM (GB)"] or an empty list
    }
}
