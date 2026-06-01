package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.requests.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.requests.GetMyAuctionsRequest;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.SignupRequest;
import com.groupproject.shared.network.responses.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.responses.CreateAuctionResponse;
import com.groupproject.shared.network.responses.LoginResponse;
import com.groupproject.shared.network.responses.Response;
import com.groupproject.shared.network.responses.SignupResponse;

/**
 * Test Suite 06: Security & Middleware (SecurityMiddlewareTest)
 * Tập trung: Chặn truy cập trái phép, chặn leo thang đặc quyền và bảo vệ quyền sở hữu dữ liệu.
 */
public class SecurityMiddlewareTest {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final Object consoleLock = new Object();

    public static void main(String[] args) {
        logSystem("=== KHỞI CHẠY KIỂM THỬ BẢO MẬT & MIDDLEWARE (SECURITY_TEST) ===");
        
        testAnonymousAccess();
        testAdminPrivilegeEscalation();
        testOwnershipProtection();

        logSystem("=== HỆ THỐNG KIỂM THỬ SECURITY_TEST HOÀN THÀNH ===");
    }

    // --- CASE 01: Truy cập ẩn danh (Chưa đăng nhập) ---
    private static void testAnonymousAccess() {
        logAction("ANONYMOUS", "TC_SEC01: Thử lấy danh sách phòng cá nhân khi chưa Login...");
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Gửi request mà không qua bước Login
            out.writeObject(new GetMyAuctionsRequest());
            out.flush();

            Object res = in.readObject();
            if (res instanceof Response) {
                Response response = (Response) res;
                logSystem("📩 Server phản hồi: " + response.getMessage());
                if (!response.isSuccess()) {
                    logSystem("✅ ĐÚNG: Server đã chặn yêu cầu ẩn danh.");
                }
            }
        } catch (Exception e) {
            logError("TC_SEC01", "Kết nối bị ngắt hoặc lỗi: " + e.getMessage());
        }
    }

    // --- CASE 02: Leo thang đặc quyền (User thường thử làm việc của Admin) ---
    private static void testAdminPrivilegeEscalation() {
        logAction("USER_ATTACKER", "TC_SEC02: User thường thử gửi lệnh Admin (ví dụ: Xóa danh mục)...");
        String hacker = "hacker_" + System.currentTimeMillis();
        
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            setupSession(out, in, hacker, "pass123");

            // Giả sử có request xóa danh mục chỉ dành cho Admin
            // Ở đây mình dùng ChangeAuctionStatus của một ID không tồn tại nhưng với tư cách ép buộc
            // Hoặc bạn có thể thay bằng DeleteUserRequest / CreateCategoryRequest nếu có
            out.writeObject(new ChangeAuctionStatusRequest(999, AuctionStatus.CANCELLED));
            out.flush();

            Object res = in.readObject();
            if (res instanceof Response) {
                Response response = (Response) res;
                logSystem("📩 Server phản hồi: " + response.getMessage());
                // Nếu server check role Admin trước khi check ID phòng
                if (!response.isSuccess() && response.getMessage().toLowerCase().contains("quyền")) {
                    logSystem("✅ ĐÚNG: Server đã chặn hành vi leo thang đặc quyền.");
                }
            }
        } catch (Exception e) {
            logError("TC_SEC02", "Lỗi: " + e.getMessage());
        }
    }

    // --- CASE 03: Bảo vệ quyền sở hữu (User A phá phòng User B) ---
    private static void testOwnershipProtection() {
        logAction("SECURITY", "TC_SEC03: User B thử HỦY phòng của User A...");
        long ts = System.currentTimeMillis();
        String userA = "userA_" + ts;
        String userB = "userB_" + ts;
        int auctionIdOfA = -1;

        try {
            // 1. User A tạo phòng
            try (Socket sA = new Socket(SERVER_HOST, SERVER_PORT);
                 ObjectOutputStream oA = new ObjectOutputStream(sA.getOutputStream());
                 ObjectInputStream iA = new ObjectInputStream(sA.getInputStream())) {
                
                setupSession(oA, iA, userA, "pass123");
                Category cat = new Category(1, "Test", null);
                oA.writeObject(new CreateAuctionRequest("Phòng của A", "Mô tả", cat, null, 100.0, null, 
                               LocalDateTime.now().plusHours(1).toString(), AuctionStatus.WAITING));
                oA.flush();
                CreateAuctionResponse res = readExpectedResponse(iA, CreateAuctionResponse.class);
                auctionIdOfA = res.getAuction().getId();
            }

            // 2. User B nhảy vào đòi Hủy phòng của A
            try (Socket sB = new Socket(SERVER_HOST, SERVER_PORT);
                 ObjectOutputStream oB = new ObjectOutputStream(sB.getOutputStream());
                 ObjectInputStream iB = new ObjectInputStream(sB.getInputStream())) {
                
                setupSession(oB, iB, userB, "pass123");
                logAction(userB, "Đang cố gắng hủy phòng ID " + auctionIdOfA + " của User A...");
                
                oB.writeObject(new ChangeAuctionStatusRequest(auctionIdOfA, AuctionStatus.CANCELLED));
                oB.flush();

                Object res = iB.readObject();
                if (res instanceof ChangeAuctionStatusResponse) {
                    ChangeAuctionStatusResponse statusRes = (ChangeAuctionStatusResponse) res;
                    logSystem("📩 Server phản hồi: " + statusRes.getMessage());
                    if (!statusRes.isSuccess()) {
                        logSystem("✅ ĐÚNG: Server bảo vệ quyền sở hữu, không cho phép người lạ can thiệp phòng.");
                    } else {
                        logError("SECURITY", "❌ LỖI NGHIÊM TRỌNG: User B đã hủy được phòng của User A!");
                    }
                }
            }
        } catch (Exception e) {
            logError("TC_SEC03", "Lỗi: " + e.getMessage());
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH
    // =========================================================================

    private static void setupSession(ObjectOutputStream out, ObjectInputStream in, String user, String pass) throws Exception {
        out.writeObject(new SignupRequest(user, user + "@test.com", pass, pass)); out.flush();
        readExpectedResponse(in, SignupResponse.class);
        out.writeObject(new LoginRequest(user, pass)); out.flush();
        readExpectedResponse(in, LoginResponse.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readExpectedResponse(ObjectInputStream in, Class<T> expectedClass) throws Exception {
        while (true) {
            Object obj = in.readObject();
            if (expectedClass.isInstance(obj)) return expectedClass.cast(obj);
        }
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
}