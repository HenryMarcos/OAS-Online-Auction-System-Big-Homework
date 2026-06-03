package com.groupproject.shared.model.transaction;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuctionDetailTest {

    @Test
    void testAuctionDetailConstructorAndGettersSetters() {
        // Mock dữ liệu đầu vào
        Auction mockAuction = new Auction(1, 1, "Test", null, null, 10.0, 100L, null, null, null);
        List<byte[]> subImageBytesList = new ArrayList<>();
        subImageBytesList.add(new byte[]{1, 2});
        
        List<BidDTO> bidHistory = new ArrayList<>();
        bidHistory.add(new BidDTO("User1", 20.0, null));

        Map<Integer, Map<String, String>> specs = new HashMap<>();

        // Khởi tạo
        AuctionDetail detail = new AuctionDetail(mockAuction, "Great item", subImageBytesList, bidHistory, specs);

        // Test Getters
        assertEquals(mockAuction, detail.getAuction());
        assertEquals("Great item", detail.getDescription());
        assertEquals(subImageBytesList, detail.getSubImageBytesList());
        assertEquals(bidHistory, detail.getBidHistory());
        assertEquals(specs, detail.getCategoryGroupedSpecs());
        assertNotNull(detail.getSubImagePaths()); // Được khởi tạo là ArrayList rỗng

        // Test Setters
        detail.setDescription("Updated item");
        assertEquals("Updated item", detail.getDescription());

        Map<Integer, Map<String, String>> newSpecs = new HashMap<>();
        newSpecs.put(1, new HashMap<>());
        detail.setCategoryGroupedSpecs(newSpecs);
        assertEquals(newSpecs, detail.getCategoryGroupedSpecs());

        List<String> paths = new ArrayList<>();
        paths.add("/path/to/img.png");
        detail.setSubImagePaths(paths);
        assertEquals(paths, detail.getSubImagePaths());

        List<byte[]> newSubImageBytesList = new ArrayList<>();
        newSubImageBytesList.add(new byte[]{3, 4});
        detail.setSubImageBytesList(newSubImageBytesList);
        assertEquals(newSubImageBytesList, detail.getSubImageBytesList());
    }
}