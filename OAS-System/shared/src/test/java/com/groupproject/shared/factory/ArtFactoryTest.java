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

import com.groupproject.shared.model.item.Art;
import com.groupproject.shared.model.item.Item;

public class ArtFactoryTest {

    // =================================================================
    // PHẦN 1: KIỂM THỬ TRƯỜNG HỢP THÀNH CÔNG (HAPPY PATH)
    // Mục tiêu: Xem khi truyền dữ liệu CHUẨN, máy có tạo ra đúng đồ CHUẨN không.
    // =================================================================

    // @Test báo cho máy biết đây là một kịch bản kiểm thử độc lập
    @Test
    void testCreateItem_Success() {
        // 1. Chuẩn bị (Arrange): Tạo ra cái máy sản xuất (Factory)
        ArtFactory factory = new ArtFactory();

        // 2. Thực thi (Act): Bấm nút cho máy chạy, truyền dữ liệu hoàn hảo vào
        Item item = factory.createItem("Mona Lisa", "Tranh sơn dầu", "Da Vinci");

        // 3. Kiểm tra (Assert): So sánh kết quả thực tế với kết quả kỳ vọng
        
        // Đảm bảo đồ vật tạo ra không bị rỗng (null)
        assertNotNull(item, "Item tạo ra không được phép null");
        
        // Đảm bảo đồ vật tạo ra đúng là kiểu Art (chứ không phải Electronic hay Vehicle)
        assertTrue(item instanceof Art, "Item phải là một instance của class Art");

        // Ép kiểu (Cast) từ Item chung chung thành Art cụ thể để lấy được các thuộc tính riêng
        Art art = (Art) item;
        
        // Kiểm tra xem dữ liệu có bị "rơi rụng" trong quá trình tạo không
        assertEquals("Mona Lisa", art.getName());
        assertEquals("Tranh sơn dầu", art.getDescription());
        assertEquals("Da Vinci", art.getArtist(), "Tác giả phải được lấy từ attributes[0]");
    }

    @Test
    void testCreateItem_ExtraAttributes_Success() {
        ArtFactory factory = new ArtFactory();
        
        // Cố tình truyền dư 2 tham số ("Italy", "1503") để xem code có bị lỗi không
        Item item = factory.createItem("Mona Lisa", "Tranh sơn dầu", "Da Vinci", "Italy", "1503");

        assertNotNull(item);
        Art art = (Art) item;
        
        assertEquals("Da Vinci", art.getArtist(), "Chỉ được phép lấy phần tử đầu tiên làm tác giả");
    }

    // =================================================================
    // PHẦN 2: KIỂM THỬ NGOẠI LỆ BẰNG BẢNG DỮ LIỆU (@CsvSource)
    // Mục tiêu: Cố tình truyền sai Name hoặc Description để xem máy có bắt lỗi không.
    // =================================================================

    // @ParameterizedTest thay thế cho @Test, cho phép chạy 1 hàm nhiều lần với data khác nhau
    // Thuộc tính 'name' giúp đổi tên test case khi hiển thị cho dễ đọc
    @ParameterizedTest(name = "Lỗi Name/Desc số {index}: name='{0}', desc='{1}'")
    
    // @CsvSource giống như một cái bảng Excel. Mỗi dòng là 1 lượt chạy.
    // Cột 1 truyền vào biến 'name', Cột 2 truyền vào biến 'description'
    @CsvSource(value = {
            "null, Tranh sơn dầu",   // Lượt 1: Name = null
            "'', Tranh sơn dầu",     // Lượt 2: Name = "" (Chuỗi rỗng)
            "Mona Lisa, null",       // Lượt 3: Description = null
            "Mona Lisa, ''",         // Lượt 4: Description = ""
            "null, null",            // Lượt 5: Cả 2 đều null
            "'', ''"                 // Lượt 6: Cả 2 đều rỗng
    }, nullValues = {"null"})        // Báo cho JUnit hiểu chữ "null" ở trên là giá trị null thật sự
    void testCreateItem_InvalidBaseParams_ThrowsException(String name, String description) {
        ArtFactory factory = new ArtFactory();
        
        // assertThrows: Dùng để "đón lõng" một cái lỗi.
        // Nếu đoạn code bên trong () -> {...} ném ra IllegalArgumentException, bài test PASS (Màu xanh)
        // Nếu nó KHÔNG ném ra lỗi (vẫn ráng tạo ra item), bài test FAILED (Màu đỏ)
        assertThrows(IllegalArgumentException.class, () -> {
            // Cố định artist là "Da Vinci", chỉ thay đổi name và description theo bảng trên
            factory.createItem(name, description, "Da Vinci");
        }, "Phải ném ra lỗi khi Name hoặc Description bị null/rỗng");
    }

    // =================================================================
    // PHẦN 3: KIỂM THỬ NGOẠI LỆ MẢNG DỮ LIỆU BẰNG HÀM (@MethodSource)
    // Mục tiêu: Cố tình phá mảng 'attributes' (artist) để ép code văng lỗi.
    // =================================================================

    // Vì mảng (Array) không thể viết dạng text vào @CsvSource, ta phải viết một hàm riêng
    // Hàm này sẽ nhả ra một dòng suối (Stream) chứa các loại mảng lỗi khác nhau.
    static Stream<String[]> invalidAttributesProvider() {
        return Stream.of(
                null,                       // Trường hợp 1: Không truyền mảng (truyền null)
                new String[]{},             // Trường hợp 2: Truyền mảng nhưng không có gì bên trong (length = 0)
                new String[]{null},         // Trường hợp 3: Có 1 phần tử, nhưng phần tử đó bị null
                new String[]{""},           // Trường hợp 4: Có 1 phần tử, nhưng là chuỗi rỗng
                new String[]{"   "}         // Trường hợp 5: Có 1 phần tử, nhưng toàn dấu cách
        );
    }

    // Liên kết bài test này với cái hàm cung cấp data ở trên
    @ParameterizedTest(name = "Lỗi Attributes số {index}")
    @MethodSource("invalidAttributesProvider") // Tên hàm phải gõ chính xác vào đây
    void testCreateItem_InvalidAttributes_ThrowsException(String[] invalidAttributes) {
        ArtFactory factory = new ArtFactory();

        // Bắt lỗi tương tự như phần trên
        assertThrows(IllegalArgumentException.class, () -> {
            // Cố định Name và Desc luôn đúng, chỉ ném cái mảng lỗi vào tham số cuối cùng
            factory.createItem("Mona Lisa", "Tranh sơn dầu", invalidAttributes);
        }, "Phải ném ra lỗi khi mảng Attributes bị null, rỗng, hoặc chứa phần tử sai định dạng");
    }
}