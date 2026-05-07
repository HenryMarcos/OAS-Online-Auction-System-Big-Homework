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

import com.groupproject.shared.model.item.Item;
import com.groupproject.shared.model.item.Vehicle;

class VehicleFactoryTest {

    // =================================================================
    // 1. KIỂM THỬ TRƯỜNG HỢP THÀNH CÔNG (HAPPY PATH)
    // =================================================================

    @Test
    void testCreateItem_Success() {
        VehicleFactory factory = new VehicleFactory();
        
        // Truyền đúng 2 thuộc tính: Brand="Toyota", Model="Camry"
        Item item = factory.createItem("Ô tô gia đình", "Xe 5 chỗ ngồi", "Toyota", "Camry");

        assertNotNull(item, "Item tạo ra không được phép null");
        assertTrue(item instanceof Vehicle, "Item phải là một instance của class Vehicle");

        // Giả định model Vehicle của bạn có getBrand() và getModel()
        Vehicle vehicle = (Vehicle) item;
        assertEquals("Ô tô gia đình", vehicle.getName());
        assertEquals("Xe 5 chỗ ngồi", vehicle.getDescription());
        assertEquals("Toyota", vehicle.getBrand(), "Brand phải lấy từ attributes[0]");
        assertEquals("Camry", vehicle.getModel(), "Model phải lấy từ attributes[1]");
    }

    @Test
    void testCreateItem_ExtraAttributes_Success() {
        VehicleFactory factory = new VehicleFactory();
        
        // Cố tình truyền dư tham số: "Màu Đen", "2024", "Xăng"
        Item item = factory.createItem("Ô tô gia đình", "Xe 5 chỗ ngồi", "Toyota", "Camry", "Màu Đen", "2024");

        assertNotNull(item);
        Vehicle vehicle = (Vehicle) item;
        
        // Code chỉ được phép lấy đúng 2 phần tử đầu làm Brand và Model, bỏ qua phần thừa
        assertEquals("Toyota", vehicle.getBrand());
        assertEquals("Camry", vehicle.getModel());
    }

    // =================================================================
    // 2. KIỂM THỬ NGOẠI LỆ: THIẾU HOẶC SAI THÔNG TIN CƠ BẢN (NAME, DESCRIPTION)
    // =================================================================

    @ParameterizedTest(name = "Lỗi Name/Desc số {index}: name='{0}', desc='{1}'")
    @CsvSource(value = {
            "null, Xe 5 chỗ ngồi",
            "'', Xe 5 chỗ ngồi",
            "Ô tô gia đình, null",
            "Ô tô gia đình, ''",
            "null, null",
            "'', ''"
    }, nullValues = {"null"})
    void testCreateItem_InvalidBaseParams_ThrowsException(String name, String description) {
        VehicleFactory factory = new VehicleFactory();
        
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem(name, description, "Toyota", "Camry");
        }, "Phải ném ra lỗi khi Name hoặc Description bị null/rỗng");
    }

    // =================================================================
    // 3. KIỂM THỬ NGOẠI LỆ: THIẾU HOẶC SAI THÔNG TIN (ATTRIBUTES)
    // =================================================================

    // Nhà máy sản xuất mảng dữ liệu lỗi (Cần 2 tham số, nếu thiếu sẽ lỗi)
    static Stream<String[]> invalidAttributesProvider() {
        return Stream.of(
                null,                             // Mảng null
                new String[]{},                   // Mảng rỗng (length = 0)
                new String[]{"Toyota"},           // Chỉ có 1 phần tử (length = 1) -> Chắc chắn tạch
                new String[]{null, "Camry"},      // Đủ 2 phần tử nhưng Brand bị null
                new String[]{"Toyota", null},     // Đủ 2 phần tử nhưng Model bị null
                new String[]{"", "Camry"},        // Brand là chuỗi rỗng
                new String[]{"Toyota", "   "}     // Model toàn khoảng trắng
        );
    }

    @ParameterizedTest(name = "Lỗi Attributes số {index}")
    @MethodSource("invalidAttributesProvider")
    void testCreateItem_InvalidAttributes_ThrowsException(String[] invalidAttributes) {
        VehicleFactory factory = new VehicleFactory();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem("Ô tô gia đình", "Xe 5 chỗ", invalidAttributes);
        });

        // Bắt chính xác câu chữ lỗi mà bạn đã viết trong VehicleFactory.java
        assertEquals("Phải có hai thuộc tính để tạo đối tượng Vehicle: brand và model", exception.getMessage());
    }
}