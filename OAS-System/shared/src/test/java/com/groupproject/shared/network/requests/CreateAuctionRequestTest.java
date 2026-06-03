package com.groupproject.shared.network.requests;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class CreateAuctionRequestTest {

    @Test
    public void testCreateAuctionRequestConstructorAndGetters() { // 🌟 THÊM PUBLIC VÀO ĐÂY
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime endTime = startTime.plusDays(2);
        byte[] mainImage = new byte[]{1, 2, 3};
        List<byte[]> subImages = new ArrayList<>();
        subImages.add(new byte[]{4, 5});
        Map<Integer, Map<String, String>> specs = new HashMap<>();

        // Khởi tạo đối tượng test (truyền null cho các class chưa rõ implementation như Category, AuctionStatus)
        CreateAuctionRequest request = new CreateAuctionRequest(
            "Laptop Dell XPS",
            "Mới 99%, nguyên hộp",
            null, 
            specs,
            mainImage,
            subImages,
            1500.0,
            172800L,
            startTime,
            endTime,
            null
        );

        // Kiểm tra tất cả các hàm Getter để đạt 100% Coverage
        assertEquals("Laptop Dell XPS", request.getTitle());
        assertEquals("Mới 99%, nguyên hộp", request.getDescription());
        assertNull(request.getCategory());
        assertEquals(specs, request.getCategoryGroupedSpecs());
        assertArrayEquals(mainImage, request.getMainImageBytes());
        assertEquals(subImages, request.getSubImagesBytes());
        assertEquals(1500.0, request.getStartingPrice());
        assertEquals(172800L, request.getDuration());
        assertEquals(startTime, request.getStartTime());
        assertEquals(endTime, request.getEndTime());
        assertNull(request.getStatus());
    }
}