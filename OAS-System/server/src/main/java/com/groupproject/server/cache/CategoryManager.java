package com.groupproject.server.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;

public enum CategoryManager {
    INSTANCE;

    // Lưu tất cả vào map bằng id để có tốc độ tìm O(1)
    private Map<Integer, Category> categoryMap;
    private List<Category> mainCategoryList;

    // Khóa đọc/ghi đảm bảo các máy khách có thể đọc đồng thời,
    // nhưng việc đọc chỉ bị chặn trong tích tắc khi bộ nhớ đệm đang được cập nhật
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private CategoryManager() {
        this.categoryMap = new ConcurrentHashMap<>();
        this.mainCategoryList = new ArrayList<>();
        refreshCache(); // Load từ database lúc khởi tạo
    }

    /*
    Gọi hàm này mỗi khi server chạy, hoặc khi admin chỉnh sửa category trong database
    */
    public void refreshCache() {
        lock.writeLock().lock();
        try {
            ServerLogger.info("Refreshing Category Cache from database...");

            Map<Integer, Category> categories = CategoryDAO.getCategories();
            List<Category> mainCategories = CategoryDAO.getMainCategories(categories);

            this.categoryMap.clear();
            this.categoryMap.putAll(categories);

            this.mainCategoryList.clear();
            this.mainCategoryList.addAll(mainCategories);

            ServerLogger.info("Category Cache refreshed successfully.");
        } catch (Exception e) {
            ServerLogger.error("Failed to refresh category cache: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Category getCategory(int id) {
        lock.readLock().lock();
        try {
            return categoryMap.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<Integer, Category> getCategories() {
        lock.readLock().lock();
        try {
            return categoryMap;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Category> getMainCategories() {
        lock.readLock().lock();
        try {
            return mainCategoryList;
        } finally {
            lock.readLock().unlock();
        }
    }
}
