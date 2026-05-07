package com.groupproject.shared.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.groupproject.shared.model.item.Art;
import com.groupproject.shared.model.item.Electronic;
import com.groupproject.shared.model.item.Item;

class ItemFactoryTest {

    // =================================================================
    // 1. KIỂM THỬ GỌI ĐÚNG FACTORY (HAPPY PATH)
    // =================================================================

    @Test
    void testCreateItem_ArtType_ReturnsArtInstance() {
        // Vì hàm createItem là static, ta gọi trực tiếp từ Class, không cần dùng "new"
        Item item = ItemFactory.createItem("art", "Mona Lisa", "Tranh sơn dầu", "Da Vinci");

        assertNotNull(item);
        assertTrue(item instanceof Art, "Phải trả về đúng đối tượng Art");
    }

    @Test
    void testCreateItem_ElectronicType_ReturnsElectronicInstance() {
        Item item = ItemFactory.createItem("electronic", "PS5", "Console", "Sony", "Playstation 5");

        assertNotNull(item);
        assertTrue(item instanceof Electronic, "Phải trả về đúng đối tượng Electronic");
    }

    // =================================================================
    // 2. KIỂM THỬ XỬ LÝ CHỮ HOA/CHỮ THƯỜNG & KHOẢNG TRẮNG
    // (Test dòng code: type.trim().toLowerCase())
    // =================================================================

    @ParameterizedTest(name = "Xử lý định dạng chuỗi: type = '{0}'")
    @ValueSource(strings = {
            "ART",          // Chữ in hoa
            "aRt",          // Chữ hoa chữ thường lẫn lộn
            "  art  ",      // Có khoảng trắng 2 đầu
            "   ART   "     // Vừa in hoa vừa có khoảng trắng
    })
    void testCreateItem_ValidTypeWithMessyFormat_Success(String messyType) {
        // Dù người dùng nhập kiểu gì ở trên, nó vẫn phải hiểu là "art" và tạo thành công
        Item item = ItemFactory.createItem(messyType, "Mona Lisa", "Tranh", "Da Vinci");
        
        assertNotNull(item);
        assertTrue(item instanceof Art);
    }

    // =================================================================
    // 3. KIỂM THỬ NGOẠI LỆ: LOẠI SẢN PHẨM KHÔNG TỒN TẠI
    // =================================================================

    @Test
    void testCreateItem_InvalidType_ThrowsException() {
        // Truyền vào một loại đồ vật không có trong Map (VD: "book")
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("book", "Sách Lập Trình", "Học Java", "Tác giả A");
        });

        // Kiểm tra xem câu thông báo lỗi có đúng như trong file ItemFactory.java không
        assertEquals("Loại sản phẩm không hợp lệ: book", exception.getMessage());
    }

    // =================================================================
    // 4. KIỂM THỬ NGOẠI LỆ: TYPE BỊ NULL (LỖI TIỀM ẨN TRONG CODE CỦA BẠN)
    // =================================================================

    @Test
    void testCreateItem_NullType_ThrowsNullPointerException() {
        // ⚠️ Chú ý: Ở dòng 18 code của bạn là `type.trim().toLowerCase()`.
        // Nếu `type` bị null, việc gọi `.trim()` sẽ gây ra lỗi NullPointerException.
        assertThrows(NullPointerException.class, () -> {
            ItemFactory.createItem(null, "Tên", "Mô tả", "Thuộc tính");
        });
    }
}