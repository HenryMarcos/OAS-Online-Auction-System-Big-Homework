package com.groupproject.shared.factory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.groupproject.shared.model.item.Electronic;
import com.groupproject.shared.model.item.Item;

class ElectronicFactoryTest {

    // =================================================================
    // 1. KIỂM THỬ TRƯỜNG HỢP THÀNH CÔNG (HAPPY PATH)
    // =================================================================

    @Test
    void testCreateItem_Success() {
        ElectronicFactory factory = new ElectronicFactory();
        
        // Truyền đúng 2 thuộc tính: Brand="Sony", Model="PS5"
        Item item = factory.createItem("Máy chơi game", "Console thế hệ mới", "Sony", "PS5");

        assertNotNull(item, "Item tạo ra không được phép null");
        assertTrue(item instanceof Electronic, "Item phải là một instance của class Electronic");

        // Giả định model Electronic của bạn có getBrand() và getModel()
        Electronic electronic = (Electronic) item;
        assertEquals("Máy chơi game", electronic.getName());
        assertEquals("Console thế hệ mới", electronic.getDescription());
        assertEquals("Sony", electronic.getBrand(), "Brand phải là attributes[0]");
        assertEquals("PS5", electronic.getModel(), "Model phải là attributes[1]");
    }

    @Test
    void testCreateItem_ExtraAttributes_Success() {
        ElectronicFactory factory = new ElectronicFactory();
        
        // Cố tình truyền dư tham số: "Màu Trắng", "2024"
        Item item = factory.createItem("Máy chơi game", "Console thế hệ mới", "Sony", "PS5", "Màu Trắng", "2024");

        assertNotNull(item);
        Electronic electronic = (Electronic) item;
        
        // Code chỉ được phép lấy đúng 2 phần tử đầu, bỏ qua phần thừa
        assertEquals("Sony", electronic.getBrand());
        assertEquals("PS5", electronic.getModel());
    }

    // =================================================================
    // 2. KIỂM THỬ NGOẠI LỆ: THIẾU HOẶC SAI THÔNG TIN (NAME, DESCRIPTION)
    // =================================================================

    @ParameterizedTest(name = "Lỗi Name/Desc số {index}: name='{0}', desc='{1}'")
    @CsvSource(value = {
            "null, Console thế hệ mới",
            "'', Console thế hệ mới",
            "Máy chơi game, null",
            "Máy chơi game, ''",
            "null, null",
            "'', ''"
    }, nullValues = {"null"})
    void testCreateItem_InvalidBaseParams_ThrowsException(String name, String description) {
        ElectronicFactory factory = new ElectronicFactory();
        
        assertThrows(IllegalArgumentException.class, () -> {
            // Chỉ phá Name và Desc, truyền đúng Brand và Model
            factory.createItem(name, description, "Sony", "PS5");
        }, "Phải ném ra lỗi khi Name hoặc Description bị null/rỗng");
    }

    // =================================================================
    // 3. KIỂM THỬ NGOẠI LỆ: THIẾU HOẶC SAI THÔNG TIN (ATTRIBUTES)
    // =================================================================

    // Nhà máy sản xuất mảng dữ liệu lỗi (Đã điều chỉnh cho trường hợp cần 2 tham số)
    static Stream<String[]> invalidAttributesProvider() {
        return Stream.of(
                null,                           // Mảng null
                new String[]{},                 // Mảng rỗng (length = 0)
                new String[]{"Sony"},           // Chỉ có 1 phần tử (length = 1 -> Chắc chắn tạch vì cần 2)
                new String[]{null, "PS5"},      // Đủ 2 phần tử nhưng Brand bị null
                new String[]{"Sony", null},     // Đủ 2 phần tử nhưng Model bị null
                new String[]{"", "PS5"},        // Brand là chuỗi rỗng
                new String[]{"Sony", "   "}     // Model toàn khoảng trắng
        );
    }

    @ParameterizedTest(name = "Lỗi Attributes số {index}")
    @MethodSource("invalidAttributesProvider")
    void testCreateItem_InvalidAttributes_ThrowsException(String[] invalidAttributes) {
        ElectronicFactory factory = new ElectronicFactory();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem("Máy chơi game", "Console thế hệ mới", invalidAttributes);
        });

        // Bắt chính xác câu chữ lỗi mà bạn đã viết trong ElectronicFactory.java
        assertEquals("Phải có hai thuộc tính để tạo đối tượng Electronic: brand và model", exception.getMessage());
    }
}