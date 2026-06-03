package com.groupproject.server.dao;

import com.groupproject.shared.model.categories.Category;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CategoryDAOTest {

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    public void setUp() throws Exception {
        // 1. Khởi tạo các Mock object
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // 2. Nuốt mọi câu lệnh SQL (anyString) để chống lỗi chính tả
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // 3. Ghi đè DatabaseManager giống y hệt như các file Test trước
        HikariDataSource mockDataSource = mock(HikariDataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);

        for (Field field : DatabaseManager.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType().getName().contains("DataSource") || field.getType().getName().contains("Pool")) {
                field.set(DatabaseManager.INSTANCE, mockDataSource);
            } else if (field.getType() == Connection.class) {
                field.set(DatabaseManager.INSTANCE, mockConnection);
            }
        }
    }

    @Test
    public void testGetCategories() throws Exception {
        when(mockResultSet.next()).thenReturn(true, true, true, false);
        
        when(mockResultSet.getInt("id")).thenReturn(1, 1, 2);
        when(mockResultSet.getString("name")).thenReturn("Điện tử", "Điện tử", "Laptop");
        
        when(mockResultSet.getObject("parent_id")).thenReturn(null, null, 1);
        
        // 🌟 SỬA TẠI ĐÂY: Vì getInt chỉ được code thật gọi duy nhất 1 lần (khi Object khác null)
        // nên ta chỉ cần Mock trả về thẳng số 1 là xong.
        when(mockResultSet.getInt("parent_id")).thenReturn(1); 
        
        when(mockResultSet.getString("field_name")).thenReturn("Brand", "Warranty", "RAM");

        Map<Integer, Category> categories = CategoryDAO.getCategories();

        assertEquals(2, categories.size());
        
        Category cat1 = categories.get(1);
        assertNotNull(cat1);
        assertEquals("Điện tử", cat1.getName());
        assertNull(cat1.getParentId());
        
        Category cat2 = categories.get(2);
        assertNotNull(cat2);
        // Bây giờ nó sẽ nhận đúng số 1
        assertEquals(1, cat2.getParentId()); 
    }

    @Test
    public void testGetMainCategoriesNoArgs() throws Exception {
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(10, 20);
        when(mockResultSet.getString("name")).thenReturn("Sách", "Tiểu thuyết");
        
        when(mockResultSet.getObject("parent_id")).thenReturn(null, 10);
        // 🌟 SỬA TẠI ĐÂY TƯƠNG TỰ: Trả về thẳng ID parent là 10
        when(mockResultSet.getInt("parent_id")).thenReturn(10);
        
        when(mockResultSet.getString("field_name")).thenReturn(null, null);

        List<Category> mainCategories = CategoryDAO.getMainCategories();

        assertEquals(1, mainCategories.size());
        assertEquals("Sách", mainCategories.get(0).getName());
    }

    @Test
    public void testGetMainCategoriesWithMapParameter() {
        Map<Integer, Category> mockMap = new HashMap<>();
        
        Category parent1 = new Category(1, "Xe cộ", null);
        Category child1 = new Category(2, "Xe máy", 1);
        Category parent2 = new Category(3, "Thời trang", null);

        mockMap.put(1, parent1);
        mockMap.put(2, child1);
        mockMap.put(3, parent2);

        List<Category> mainCategories = CategoryDAO.getMainCategories(mockMap);

        assertEquals(2, mainCategories.size());
        assertTrue(mainCategories.contains(parent1));
        assertTrue(mainCategories.contains(parent2));
    }
}