package com.groupproject.test;

import com.groupproject.shared.factory.ItemFactory;
import com.groupproject.shared.model.item.Art;
import com.groupproject.shared.model.item.Item;
import com.groupproject.shared.model.user.Bidder;
import com.groupproject.shared.model.user.Seller;

public class MainTest {
    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU KIỂM THỬ HỆ THỐNG ===\n");

        // ---------------------------------------------------------
        // KỊCH BẢN 1: KIỂM TRA LỚP USER (BIDDER & SELLER)
        // ---------------------------------------------------------
        System.out.println("1. Kiểm tra Bidder và Seller:");
        Bidder bidder = new Bidder(0.0, "123 Đường ABC", "nguoimua1", "pass123", "mua@gmail.com");
        bidder.deposit(500000); // Nạp 500k
        System.out.println("- Số dư của Bidder sau khi nạp: " + bidder.getAccountBalance());

        Seller seller = new Seller("nguoiban1", "pass456", "ban@gmail.com");
        seller.updateRating(4.5f);
        seller.updateRating(5.0f);
        System.out.println("- Đánh giá trung bình của Seller: " + seller.getRating() + " (Số lượt: " + seller.getRatingNumber() + ")");
        System.out.println();

        // ---------------------------------------------------------
        // KỊCH BẢN 2: KIỂM TRA FACTORY METHOD & LÀM SẠCH DỮ LIỆU
        // ---------------------------------------------------------
        System.out.println("2. Kiểm tra ItemFactory:");
        // Truyền chữ "  aRt  " luộm thuộm xem Factory có tự dọn dẹp và tạo đúng không
        Item myArt = ItemFactory.createItem(
                "  aRt  ", // Type
                "Bức tranh mùa thu", // Name
                1000000, // Giá khởi điểm
                seller.getId(), // Lấy ID của seller vừa tạo ở trên
                "Tranh sơn dầu thế kỷ 19", // Mô tả
                "Van Gogh" // Các thuộc tính riêng của Art
        );
        
        System.out.println("- Đã tạo thành công sản phẩm: " + myArt.getTitle());
        System.out.println("- Class thực sự của đối tượng này là: " + myArt.getClass().getSimpleName());
        
        // Ép kiểu về Art để lấy thuộc tính riêng in ra thử
        if (myArt instanceof Art) {
            Art artDetail = (Art) myArt;
            System.out.println("- Tác giả bức tranh là: " + artDetail.getArtist());
        }
        System.out.println();

        // ---------------------------------------------------------
        // KỊCH BẢN 3: KIỂM TRA BẮT LỖI (EXCEPTION HANDLING)
        // ---------------------------------------------------------
        System.out.println("3. Kiểm tra các trường hợp cố tình nhập sai:");
        
        // Thử nạp tiền âm cho Bidder
        try {
            System.out.print("- Thử nạp số tiền âm: ");
            bidder.deposit(-10000);
        } catch (IllegalArgumentException e) {
            System.out.println("Bị chặn thành công! Lỗi: " + e.getMessage());
        }

        // Thử tạo một loại sản phẩm không tồn tại qua Factory
        try {
            System.out.print("- Thử tạo sản phẩm loại 'gachngoi': ");
            Item invalidItem = ItemFactory.createItem("gachngoi", "Gạch", 5000, "S01", "Mô tả", "Đỏ");
        } catch (IllegalArgumentException e) {
            System.out.println("Bị chặn thành công! Lỗi: " + e.getMessage());
        }
        
        System.out.println("\n=== HOÀN TẤT KIỂM THỬ ===");
    }
}