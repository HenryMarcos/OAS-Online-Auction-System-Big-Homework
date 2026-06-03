package com.groupproject.server.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.shared.model.categories.Category;

public class CategoryManagerTest {

    private MockedStatic<CategoryDAO> mockedCategoryDAO;

    @BeforeEach
    public void setUp() {
        // Mở giả lập static trước mỗi hàm test
        mockedCategoryDAO = mockStatic(CategoryDAO.class);
    }

    @AfterEach
    public void tearDown() {
        // Giải phóng bộ nhớ giả lập static
        if (mockedCategoryDAO != null) {
            mockedCategoryDAO.close();
        }
    }

    @Test
    public void testRefreshCacheSuccess() {
        // 1. Tạo dữ liệu danh mục giả lập chuẩn kiểu dữ liệu (List và Map)
        List<Category> fakeMainCategories = new ArrayList<>();
        Map<Integer, Category> fakeAllCategories = new HashMap<>();
        
        Category mockCategory = mock(Category.class);
        when(mockCategory.getId()).thenReturn(1);

        fakeMainCategories.add(mockCategory);
        fakeAllCategories.put(1, mockCategory);

        // 🌟 SỬA TẠI ĐÂY: Trả về đúng kiểu dữ liệu tương ứng của từng hàm trong DAO
        mockedCategoryDAO.when(CategoryDAO::getMainCategories).thenReturn(fakeMainCategories);
        mockedCategoryDAO.when(CategoryDAO::getCategories).thenReturn(fakeAllCategories);

        // 2. Kích hoạt logic cache
        CategoryManager categoryManager = CategoryManager.INSTANCE;
        assertDoesNotThrow(() -> categoryManager.refreshCache());

        // 3. Kiểm tra kết quả đảm bảo dữ liệu giả lập đã được nạp ổn định
        assertNotNull(categoryManager.getMainCategories(), "Danh sách cache không được null");
    }

    @Test
    public void testRefreshCacheFailure() {
        // Giả lập kịch bản lỗi kết nối CSDL nặng nề cho cả 2 hàm
        mockedCategoryDAO.when(CategoryDAO::getMainCategories)
                .thenThrow(new RuntimeException("Lỗi kết nối CSDL giả lập"));
        mockedCategoryDAO.when(CategoryDAO::getCategories)
                .thenThrow(new RuntimeException("Lỗi kết nối CSDL giả lập"));

        CategoryManager categoryManager = CategoryManager.INSTANCE;

        // Server phải bắt lỗi êm đẹp, không được văng crash ứng dụng
        assertDoesNotThrow(() -> categoryManager.refreshCache());
    }
}