package com.groupproject.server.cache;

import java.util.ArrayList;import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    // 3. Khởi tạo riêng tư (Private Constructor)
    private CategoryManager() {
        this.categoryMap = new ConcurrentHashMap<>();
        this.mainCategoryList = new ArrayList<>();
        refreshCache(); // Load từ database lúc khởi tạo
    }

    /*
    Gọi hàm này mỗi khi server chạy, hoặc khi admin chỉnh sửa category trong database
    */    public void refreshCache() {
        try {
            ServerLogger.info("Refreshing Category Cache từ Database (HikariCP)...");

            // Lấy dữ liệu từ DAO (Thực hiện nhanh để giải phóng Connection cho HikariCP)
            Map<Integer, Category> categories = CategoryDAO.getCategories();
            List<Category> mainCategories = CategoryDAO.getMainCategories(categories);

            if (categories != null && !categories.isEmpty()) {
                // Thay đổi dữ liệu một cách an toàn bằng việc clear và putAll
                this.categoryMap.clear();
                this.categoryMap.putAll(categories);

            this.mainCategoryList.clear();
            this.mainCategoryList.addAll(mainCategories);
                ServerLogger.info("Category Cache đã được cập nhật thành công.");
            }
        } catch (Exception e) {
            ServerLogger.error("Lỗi khi làm mới category cache: " + e.getMessage());
        }
    }

    // Lấy 1 category theo ID: An toàn, không cần lock vì Map là Concurrent
    public Category getCategory(int id) {
        return categoryMap.get(id);
    }

    /**
     * Bọc unmodifiableMap để ngăn các Handler hoặc luồng khác từ bên ngoài 
     * vô tình thực hiện thao tác xóa/sửa dữ liệu gốc của Cache.
     */
    public Map<Integer, Category> getCategories() {
        return Collections.unmodifiableMap(categoryMap);
    }

    /**
     * Trả về danh sách main categories không thể chỉnh sửa từ bên ngoài
     */
    public List<Category> getMainCategories() {
        lock.readLock().lock();
        try {
            return mainCategoryList;
        } finally {
            lock.readLock().unlock();
        }    }
}