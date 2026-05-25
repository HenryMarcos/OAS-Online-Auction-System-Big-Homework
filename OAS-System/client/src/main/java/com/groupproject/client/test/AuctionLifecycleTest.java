package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.request.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.request.CreateAuctionRequest;
import com.groupproject.shared.network.request.LoginRequest;
import com.groupproject.shared.network.request.SignupRequest;
import com.groupproject.shared.network.response.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.response.CreateAuctionResponse;
import com.groupproject.shared.network.response.LoginResponse;
import com.groupproject.shared.network.response.SignupResponse;

/**
 * Test Suite 03: Auction Lifecycle (AuctionLifecycleTest)
 * Tập trung: Kích hoạt phòng, Hủy phòng, Logic lố giờ (<60s) và Quyền hạn can thiệp.
 */
public class AuctionLifecycleTest {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final Object consoleLock = new Object();

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    public static void main(String[] args) {
        logSystem("=== KHỞI CHẠY KIỂM THỬ VÒNG ĐỜI PHÒNG ĐẤU GIÁ (LIFECYCLE_TEST) ===");

        testHappyPathAndAdminCancel();
        testActivateTooCloseToEndTime();
        testUnauthorizedStatusChange();

        logSystem("=== HỆ THỐNG KIỂM THỬ LIFECYCLE_TEST HOÀN THÀNH ===");
    }

    // --- CASE 01: Seller kích hoạt phòng hợp lệ, Admin ép hủy ---
    private static void testHappyPathAndAdminCancel() {
        logAction("LIFECYCLE", "TC_CAS01 & TC_CAS04: Seller mở phòng, Admin dùng quyền ép hủy...");
        String sellerUser = "seller_lf_" + System.currentTimeMillis();

        try (Socket sockSeller = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream outSeller = new ObjectOutputStream(sockSeller.getOutputStream());
             ObjectInputStream inSeller = new ObjectInputStream(sockSeller.getInputStream());

             Socket sockAdmin = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream outAdmin = new ObjectOutputStream(sockAdmin.getOutputStream());
             ObjectInputStream inAdmin = new ObjectInputStream(sockAdmin.getInputStream())) {

            // 1. Seller đăng nhập & tạo phòng
            setupSession(outSeller, inSeller, sellerUser, "pass123");
            Category dummyCat = new Category(1, "Art", null);
            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "Tượng gỗ nghệ thuật", "Đẹp 99%", dummyCat, null, 1000.0, null, 
                LocalDateTime.now().plusHours(1).toString(), AuctionStatus.WAITING
            );
            outSeller.writeObject(createReq); outSeller.flush();
            CreateAuctionResponse createRes = readExpectedResponse(inSeller, CreateAuctionResponse.class);
            int auctionId = createRes.getAuction().getId();

            // 2. Seller KÍCH HOẠT (ACTIVED)
            logAction(sellerUser, "Tiến hành kích hoạt phòng (WAITING -> ACTIVED) ID: " + auctionId);
            outSeller.writeObject(new ChangeAuctionStatusRequest(auctionId, AuctionStatus.ACTIVED));
            outSeller.flush();
            ChangeAuctionStatusResponse actRes = readExpectedResponse(inSeller, ChangeAuctionStatusResponse.class);
            describeStatusResponse(actRes, "TC_CAS01: KÍCH HOẠT");
            if (actRes.isSuccess()) logSystem("✅ ĐÚNG: Hệ thống cho phép Seller kích hoạt phòng hợp lệ.");

            // 3. Admin đăng nhập và HỦY PHÒNG (CANCELLED)
            logAction("ADMIN", "Admin phát hiện phòng vi phạm, tiến hành HỦY...");
            outAdmin.writeObject(new LoginRequest(ADMIN_USER, ADMIN_PASS)); outAdmin.flush();
            readExpectedResponse(inAdmin, LoginResponse.class);

            outAdmin.writeObject(new ChangeAuctionStatusRequest(auctionId, AuctionStatus.CANCELLED));
            outAdmin.flush();
            ChangeAuctionStatusResponse cancelRes = readExpectedResponse(inAdmin, ChangeAuctionStatusResponse.class);
            describeStatusResponse(cancelRes, "TC_CAS04: ADMIN HỦY");
            if (cancelRes.isSuccess()) logSystem("✅ ĐÚNG: Admin có quyền tối cao hủy phòng đang chạy.");

        } catch (Exception e) {
            logError("TC_CAS01", "Lỗi: " + e.getMessage());
        }
    }

    // --- CASE 02: Kích hoạt lố giờ (< 60s) ---
    private static void testActivateTooCloseToEndTime() {
        logAction("LIFECYCLE", "TC_CAS02: Test kích hoạt khi EndTime còn lại < 60s (Phải bị ép hủy)...");
        String sellerUser = "seller_late_" + System.currentTimeMillis();

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            setupSession(out, in, sellerUser, "pass123");

            // Tạo phòng có EndTime chỉ cách hiện tại 30 giây
            Category cat = new Category(1, "Electronics", null);
            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "Phòng test lố giờ", "Mô tả", cat, null, 100.0, null, 
                LocalDateTime.now().plusSeconds(30).toString(), AuctionStatus.WAITING
            );
            out.writeObject(createReq); out.flush();
            CreateAuctionResponse createRes = readExpectedResponse(in, CreateAuctionResponse.class);
            int badAuctionId = createRes.getAuction().getId();

            // Cố gắng kích hoạt
            logAction(sellerUser, "Cố gắng kích hoạt phòng chỉ còn 30s là kết thúc...");
            out.writeObject(new ChangeAuctionStatusRequest(badAuctionId, AuctionStatus.ACTIVED));
            out.flush();

            ChangeAuctionStatusResponse res = readExpectedResponse(in, ChangeAuctionStatusResponse.class);
            describeStatusResponse(res, "TC_CAS02: LỐ GIỜ");
            
            if (!res.isSuccess()) {
                logSystem("✅ HỆ THỐNG XỬ LÝ ĐÚNG: Đã chặn kích hoạt lố giờ và hủy phòng để bảo vệ người mua.");
            } else {
                logError("TC_CAS02", "❌ LỖI NGHIÊM TRỌNG: Server vẫn cho phép mở phòng < 60s!");
            }

        } catch (Exception e) {
            logError("TC_CAS02", "Lỗi: " + e.getMessage());
        }
    }

    // --- CASE 03: Bảo mật (Người lạ cố can thiệp phòng) ---
    private static void testUnauthorizedStatusChange() {
        logAction("LIFECYCLE", "TC_CAS03: User khác (không phải chủ phòng) thử HỦY phòng...");
        long ts = System.currentTimeMillis();
        String owner = "owner_" + ts;
        String attacker = "attacker_" + ts;

        try (Socket sockOwner = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream outOwner = new ObjectOutputStream(sockOwner.getOutputStream());
             ObjectInputStream inOwner = new ObjectInputStream(sockOwner.getInputStream());

             Socket sockAttacker = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream outAttacker = new ObjectOutputStream(sockAttacker.getOutputStream());
             ObjectInputStream inAttacker = new ObjectInputStream(sockAttacker.getInputStream())) {

            // Owner tạo phòng
            setupSession(outOwner, inOwner, owner, "pass123");
            Category cat = new Category(1, "Toys", null);
            outOwner.writeObject(new CreateAuctionRequest("Phòng của Owner", "", cat, null, 50.0, null, 
                                 LocalDateTime.now().plusHours(2).toString(), AuctionStatus.WAITING));
            outOwner.flush();
            CreateAuctionResponse resCreate = readExpectedResponse(inOwner, CreateAuctionResponse.class);
            int auctionId = resCreate.getAuction().getId();

            // Attacker đăng nhập và cố đổi trạng thái
            setupSession(outAttacker, inAttacker, attacker, "pass123");
            logAction(attacker, "Thử gửi request CANCEL phòng ID " + auctionId + " của người khác...");
            outAttacker.writeObject(new ChangeAuctionStatusRequest(auctionId, AuctionStatus.CANCELLED));
            outAttacker.flush();

            ChangeAuctionStatusResponse resAttack = readExpectedResponse(inAttacker, ChangeAuctionStatusResponse.class);
            describeStatusResponse(resAttack, "TC_CAS03: ATTACKER");

            if (!resAttack.isSuccess()) {
                logSystem("✅ ĐÚNG: Hệ thống từ chối quyền can thiệp trạng thái phòng của người lạ.");
            } else {
                logError("TC_CAS03", "❌ LỖI NGHIÊM TRỌNG: Attacker đã hủy được phòng!");
            }

        } catch (Exception e) {
            logError("TC_CAS03", "Lỗi: " + e.getMessage());
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH
    // =========================================================================

    /**
     * Hàm helper thực hiện nhanh việc Đăng ký rồi Đăng nhập trên cùng 1 Socket.
     */
    private static void setupSession(ObjectOutputStream out, ObjectInputStream in, String user, String pass) throws Exception {
        out.writeObject(new SignupRequest(user, user + "@test.com", pass, pass)); out.flush();
        readExpectedResponse(in, SignupResponse.class);
        out.writeObject(new LoginRequest(user, pass)); out.flush();
        readExpectedResponse(in, LoginResponse.class);
    }

    /**
     * Bỏ qua các Event bất đồng bộ để đọc đúng Response mong muốn.
     */
    @SuppressWarnings("unchecked")
    private static <T> T readExpectedResponse(ObjectInputStream in, Class<T> expectedClass) throws Exception {
        while (true) {
            Object obj = in.readObject();
            if (expectedClass.isInstance(obj)) return expectedClass.cast(obj);
        }
    }

    private static void describeStatusResponse(ChangeAuctionStatusResponse res, String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> CHANGE_STATUS] (%s)\n", tag));
        sb.append("   Kết quả   : ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ BỊ TỪ CHỐI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void logAction(String actor, String action) {
        synchronized (consoleLock) { System.out.println(String.format("👤 [%s] %s", actor, action)); }
    }

    private static void logSystem(String message) {
        synchronized (consoleLock) { System.out.println(String.format("🚀 [HỆ THỐNG] %s", message)); }
    }

    private static void logError(String actor, String error) {
        synchronized (consoleLock) { System.err.println(String.format("❌ [%s] LỖI: %s", actor, error)); }
    }

    private static void printLog(String fullText) {
        synchronized (consoleLock) { System.out.print(fullText); }
    }
}