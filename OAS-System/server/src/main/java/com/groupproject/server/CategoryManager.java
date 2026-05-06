package com.groupproject.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.groupproject.shared.model.categories.Category;

public class CategoryManager {
    private static final String DB_URL = "jdbc:sqlite:users.db";

    public static List<Category> getCategories() {
        // Danh sách này chỉ chứa những hạng mục chính
        List<Category> mainCategories = new ArrayList<>();

        // Tìm tất cả hạng mục bằng id
        Map<Integer, Category> categoryMap = new HashMap<>();

        String categoriesSql = "SELECT id, name, parent_id FROM categories";
        String fieldsSql = "SELECT id, category_id, field_name FROM category_fields";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement categoriesPstmt = conn.prepareStatement(categoriesSql);
             ResultSet categoriesRs = categoriesPstmt.executeQuery();
             PreparedStatement fieldsPstmt = conn.prepareStatement(fieldsSql);
             ResultSet fieldsRs = fieldsPstmt.executeQuery()) {
            
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
            System.out.println("Error fetching categories: " + e.getMessage());
        }

        for (Category category : mainCategories) {
            category.print("");
        }

        return mainCategories;
    }
}
