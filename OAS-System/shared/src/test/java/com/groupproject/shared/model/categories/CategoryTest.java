package com.groupproject.shared.model.categories;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CategoryTest {

    @Test
    public void testConstructorAndGetters() {
        // Test trường hợp parentId là null (Danh mục cha cao nhất)
        Category category = new Category(1, "Electronics", null);
        
        assertEquals(1, category.getId());
        assertEquals("Electronics", category.getName());
        assertNull(category.getParentId());
        
        // Kiểm tra danh sách con và trường yêu cầu phải được khởi tạo rỗng (không null)
        assertNotNull(category.getSubCategories());
        assertTrue(category.getSubCategories().isEmpty());
        assertNotNull(category.getRequiredFields());
        assertTrue(category.getRequiredFields().isEmpty());

        // Test trường hợp có parentId (Danh mục con)
        Category subCategory = new Category(2, "Laptops", 1);
        assertEquals(2, subCategory.getId());
        assertEquals("Laptops", subCategory.getName());
        assertEquals(1, subCategory.getParentId());
    }

    @Test
    public void testAddSubCategory() {
        Category parent = new Category(1, "Electronics", null);
        Category child = new Category(2, "Laptops", 1);

        // Thêm danh mục con
        parent.addSubCategory(child);

        List<Category> subs = parent.getSubCategories();
        assertEquals(1, subs.size());
        assertEquals(child, subs.get(0));
        assertEquals("Laptops", subs.get(0).getName());
    }

    @Test
    public void testAddRequiredField() {
        Category category = new Category(1, "Electronics", null);
        
        // Thêm các thuộc tính bắt buộc nhập
        category.addRequiredField("Warranty");
        category.addRequiredField("Brand");

        List<String> fields = category.getRequiredFields();
        assertEquals(2, fields.size());
        assertEquals("Warranty", fields.get(0));
        assertEquals("Brand", fields.get(1));
    }

    @Test
    public void testToString() {
        Category category = new Category(5, "Home Appliances", null);
        // Hàm toString() phải trả về đúng tên danh mục để hiển thị lên giao diện UI
        assertEquals("Home Appliances", category.toString());
    }

    @Test
    public void testPrintMethod() {
        // Kỹ thuật bắt luồng Output (System.out) ra bộ nhớ tạm để kiểm tra hàm void print() 
        // Giúp kiểm tra được cấu trúc đệ quy in cây danh mục đạt 100% Line Coverage
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            Category parent = new Category(1, "Electronics", null);
            parent.addRequiredField("Brand");
            
            Category child = new Category(2, "Laptops", 1);
            child.addRequiredField("RAM");
            
            parent.addSubCategory(child);

            // Gọi hàm in cấu trúc danh mục
            parent.print("");

            String output = outContent.toString();
            
            // Xác thực dữ liệu in ra Console có chính xác cấu trúc phân cấp và thuộc tính không
            assertTrue(output.contains("Electronics"));
            assertTrue(output.contains("Fields: Brand,"));
            assertTrue(output.contains("\tLaptops"));
            assertTrue(output.contains("Fields: RAM,"));
        } finally {
            // Trả lại luồng xuất chuẩn cho hệ thống sau khi test xong
            System.setOut(originalOut);
        }
    }
}