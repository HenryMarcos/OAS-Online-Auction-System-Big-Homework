package com.groupproject.server.cache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;

public class CategoryManager {

    // 1. Sử dụng ConcurrentHashMap để đọc ghi thread-safe O(1) mà không cần Lock
    private final Map<Integer, Category> categoryMap = new ConcurrentHashMap<>();
    
    // 2. Sử dụng CopyOnWriteArrayList để tối ưu cho việc đọc ghi đồng thời của List
    private final List<Category> mainCategoryList = new CopyOnWriteArrayList<>();

    // 3. Khởi tạo riêng tư (Private Constructor)
    private CategoryManager() {
        refreshCache(); // Load dữ liệu từ database lần đầu tiên khi khởi tạo
    }

    // 4. Áp dụng Bill Pugh Singleton giúp thread-safe tuyệt đối và loại bỏ 'synchronized'
    private static class Holder {
        private static final CategoryManager INSTANCE = new CategoryManager();
    }

    public static CategoryManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Làm mới bộ nhớ đệm. 
     * Do bản thân ConcurrentHashMap và CopyOnWriteArrayList đã thread-safe, 
     * ta chỉ cần nạp dữ liệu thẳng vào mà không cần giữ Lock thủ công.
     */
    public void refreshCache() {
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
        return Collections.unmodifiableList(mainCategoryList);
    }
}