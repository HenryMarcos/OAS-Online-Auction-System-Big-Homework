package com.groupproject.shared.factory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.groupproject.shared.model.item.Art;
import com.groupproject.shared.model.item.Electronic;
import com.groupproject.shared.model.item.Item;
import com.groupproject.shared.model.item.Vehicle;

public class ItemFactoryTest {

    // ================= SUCCESS CASES =================

    @Test
    public void testArtFactorySuccess() {
        Item item = ItemFactory.createItem("art", "Mona Lisa", "Famous painting", "Leonardo da Vinci");
        assertTrue(item instanceof Art, "[FAIL] ArtFactory không trả về Art");
        System.out.println("[PASS] ArtFactory tạo đúng đối tượng Art");
    }

    @Test
    public void testElectronicFactorySuccess() {
        Item item = ItemFactory.createItem("electronic", "iPhone", "Smartphone", "Apple", "iPhone 15");
        assertTrue(item instanceof Electronic, "[FAIL] ElectronicFactory không trả về Electronic");
        System.out.println("[PASS] ElectronicFactory tạo đúng đối tượng Electronic");
    }

    @Test
    public void testVehicleFactorySuccess() {
        Item item = ItemFactory.createItem("vehicle", "Car", "Luxury car", "BMW", "X5");
        assertTrue(item instanceof Vehicle, "[FAIL] VehicleFactory không trả về Vehicle");
        System.out.println("[PASS] VehicleFactory tạo đúng đối tượng Vehicle");
    }

    // ================= FAIL CASES =================

    @Test
    public void testArtFactoryFail() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("art", "Art", "Desc");
        }, "[FAIL] ArtFactory không throw exception khi thiếu attribute");
        System.out.println("[PASS] ArtFactory throw exception đúng khi thiếu attribute");
    }

    @Test
    public void testElectronicFactoryFail() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("electronic", "Phone", "Desc", "Apple");
        }, "[FAIL] ElectronicFactory không throw exception khi thiếu attribute");
        System.out.println("[PASS] ElectronicFactory throw exception đúng khi thiếu attribute");
    }

    @Test
    public void testVehicleFactoryFail() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("vehicle", "Car", "Desc", "BMW");
        }, "[FAIL] VehicleFactory không throw exception khi thiếu attribute");
        System.out.println("[PASS] VehicleFactory throw exception đúng khi thiếu attribute");
    }

    @Test
    public void testInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("unknown", "Item", "Desc");
        }, "[FAIL] Không throw exception với type không hợp lệ");
        System.out.println("[PASS] Throw exception đúng khi type không hợp lệ");
    }
}