package com.groupproject.shared.factory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.groupproject.shared.model.item.Item;
import com.groupproject.shared.model.item.Art;
import com.groupproject.shared.model.item.Electronic;
import com.groupproject.shared.model.item.Vehicle;

/*
 * Lop kiem thu ItemFactory
 * Muc tieu: Kiem tra toan bo logic tao doi tuong, bao gom ca cac truong hop dung, sai va du lieu rác.
 * Do bao phu: 100% (Coverage)
 */
class ItemFactoryTest {

    // =========================================================================
    // PHAN 1: KIEM THU CAC LUONG THANH CONG (HAPPY PATHS)
    // Muc tieu: Xac nhan Factory tao dung loai doi tuong va gan dung du lieu.
    // =========================================================================

    /*
     * Test: Tao doi tuong Art
     * Kiem tra: Factory co goi dung ArtFactory va lay dung artist tu mang attributes hay khong.
     * Cach test: Truyen "art" va kiem tra instanceOf Art.
     * Logic: ArtFactory lay attributes[0] lam artist.
     */
    @Test
    void testCreateArt_Success() {
        Item item = ItemFactory.createItem("art", "Mona Lisa", "Tranh son dau", "Da Vinci");

        assertNotNull(item, "Doi tuong khong duoc null");
        assertTrue(item instanceof Art, "Phai tao ra dung lop Art");
        
        Art art = (Art) item;
        assertEquals("Mona Lisa", art.getName(), "Ten bi sai");
        assertEquals("Da Vinci", art.getArtist(), "Ten tac gia phai lay tu attributes[0]");
    }

    /*
     * Test: Tao doi tuong Electronic
     * Kiem tra: ElectronicFactory co boc tach duoc Brand va Model tu mang hay khong.
     * Logic: attributes[0] la Brand, attributes[1] la Model.
     */
    @Test
    void testCreateElectronic_Success() {
        Item item = ItemFactory.createItem("electronic", "Laptop", "Gaming", "Asus", "ROG");

        assertTrue(item instanceof Electronic, "Phai tao ra dung lop Electronic");
        
        Electronic e = (Electronic) item;
        assertEquals("Asus", e.getBrand(), "Brand bi gan sai index trong mang");
        assertEquals("ROG", e.getModel(), "Model bi gan sai index trong mang");
    }

    /*
     * Test: Tinh linh hoat cua tham so Type
     * Kiem tra: Ham co xu ly duoc khoang trang va chu hoa/thuong hay khong.
     * Cach test: Truyen vao chuoi "  vEhIcLe  ".
     * Logic: Ben trong Factory phai dung .trim().toLowerCase() truoc khi tra cuu trong HashMap.
     */
    @Test
    void testTypeFormatting_Success() {
        Item item = ItemFactory.createItem("  vEhIcLe  ", "Car", "Desc", "Toyota", "Camry");
        
        assertNotNull(item, "Factory phai tu dong lam sach chuoi type");
        assertTrue(item instanceof Vehicle, "Phai nhan dien dung la Vehicle du viet hoa hay cach khoang");
    }

    // =========================================================================
    // PHAN 2: KIEM THU CAC TRUONG HOP LOI (EXCEPTION PATHS)
    // Muc tieu: Xac nhan he thong chan duoc du lieu sai va nem ra loi dung quy dinh.
    // =========================================================================

    /*
     * Test: Loai san pham khong ton tai
     * Kiem tra: Khi truyen mot type chua dang ky trong HashMap.
     * Logic: Factory phai nem IllegalArgumentException thay vi tra ve null de tranh loi o cac buoc sau.
     */
    @Test
    void testCreateItem_InvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("unknown_type", "Name", "Desc");
        }, "He thong phai bao loi khi loai san pham khong co trong danh sach");
    }

    /*
     * Test: Thieu tham so cho Art (Can 1)
     * Kiem tra: ArtFactory kiem tra do dai mang attributes.
     * Logic: Neu attributes.length < 1, phai bao loi thieu artist.
     */
    @Test
    void testArt_MissingAttributes() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("art", "Ten", "Mo ta"); // Khong truyen artist
        }, "ArtFactory phai chan lai khi thieu ten tac gia");
    }

    /*
     * Test: Thieu tham so cho Electronic (Can 2)
     * Kiem tra: ElectronicFactory yeu cau ca Brand va Model.
     * Logic: Neu chi truyen 1 tham so ("Apple"), attributes.length < 2 se kich hoat loi.
     */
    @Test
    void testElectronic_InsufficientAttributes() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("electronic", "Phone", "Desc", "Apple"); // Thieu model
        }, "ElectronicFactory phai chan lai khi chi co brand ma thieu model");
    }

    // =========================================================================
    // PHAN 3: KIEM THU DO BEN (ROBUSTNESS / EDGE CASES)
    // Muc tieu: Dam bao app khong bi sap (crash) khi gap du lieu "doc".
    // =========================================================================

    /*
     * Test: Type bi truyen vao la null
     * Kiem tra: Cach Factory xu ly tham so dau tien bi null.
     * Logic: Tranh loi NullPointerException (NPE) khi thuc hien .trim().
     */
    @Test
    void testCreateItem_NullType() {
        assertThrows(Exception.class, () -> {
            ItemFactory.createItem(null, "Name", "Desc");
        }, "He thong phai xu ly an toan, khong de sap app khi type bi null");
    }

    /*
     * Test: Mang attributes bi null hoan toan
     * Kiem tra: Khi nguoi dung ep kieu null cho varargs.
     * Logic: Cac Factory con nhu ArtFactory phai check attributes == null truoc khi truy cap .length.
     */
    @Test
    void testFactory_NullAttributesArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("art", "Ten", "Mo ta", (String[]) null);
        }, "He thong phai kiem tra mang null truoc khi dem do dai attributes");
    }
}