package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.shared.network.request.GetMyAuctionsRequest;
import com.groupproject.shared.network.request.LoginRequest;
import com.groupproject.shared.network.request.SignupRequest;
import com.groupproject.shared.network.response.LoginResponse;
import com.groupproject.shared.network.response.SignupResponse;

/**
 * Test Suite 01: Authentication & Authorization (AuthTest)
 * Tập trung: Đăng ký, Đăng nhập, Chặn bảo mật khi chưa đăng nhập.
 * Cơ chế: Sử dụng Admin cố định và Tự sinh User ngẫu nhiên cho mỗi lượt chạy để tránh xung đột.
 */
public class AuthTest {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final Object consoleLock = new Object();

    // Tài khoản Admin duy nhất của hệ thống (lấy từ dữ liệu Seed)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    public static void main(String[] args) {
        logSystem("=== KHỞI CHẠY HỆ THỐNG KIỂM THỬ XÁC THỰC & PHÂN QUYỀN (AUTH_TEST) ===");

        // Tạo tên tài khoản ngẫu nhiên theo thời gian chạy để không trùng với các phiên test trước hoặc file test khác
        String uniqueTimestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        String dynamicUsername = "user_" + uniqueTimestamp;
        String dynamicEmail = "user_" + uniqueTimestamp + "@test.com";
        String dynamicPassword = "password123";

        // Chạy các kịch bản kiểm thử
        testSecurityInterception();
        testSignupSuccess(dynamicUsername, dynamicEmail, dynamicPassword);
        testLoginSuccess(dynamicUsername, dynamicPassword);
        testAdminLoginSuccess();

        logSystem("=== HỆ THỐNG KIỂM THỬ AUTH_TEST HOÀN THÀNH ===");
    }

    // --- CASE 01: Kiểm tra bảo mật (Chặn khi chưa đăng nhập) ---
    private static void testSecurityInterception() {
        logAction("ANONYMOUS", "TC_SEC01: Thử lấy danh sách cá nhân khi CHƯA ĐĂNG NHẬP...");
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Gửi request lấy danh sách phòng đấu giá cá nhân ngay khi vừa kết nối
            out.writeObject(new GetMyAuctionsRequest());
            out.flush();

            Object response = in.readObject();
            // Hệ thống sẽ trả về bản tin thông báo lỗi hoặc ngắt kết nối (Tùy thuộc vào thiết kế Response lớp cha của bạn)
            logSystem("🛡️ [SECURITY] Server đã xử lý request ẩn danh. Hãy kiểm tra Log Server xem có 'SECURITY ALERT' không.");

        } catch (Exception e) {
            logError("ANONYMOUS", "Kết nối bị đóng hoặc lỗi mong đợi: " + e.getMessage());
        }
    }

    // --- CASE 02: Đăng ký tài khoản mới ---
    private static void testSignupSuccess(String username, String email, String password) {
        logAction("REGISTRATION", String.format("TC_AUT01: Tiến hành ĐĂNG KÝ tài khoản ngẫu nhiên [%s]...", username));
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            SignupRequest signupReq = new SignupRequest(username, email, password, password);
            out.writeObject(signupReq);
            out.flush();

            Object response = in.readObject();
            if (response instanceof SignupResponse) {
                describeSignupResponse((SignupResponse) response, username);
            } else {
                logError("REGISTRATION", "Nhận sai kiểu Object Response từ Server!");
            }

        } catch (Exception e) {
            logError("REGISTRATION", "Lỗi kết nối Server: " + e.getMessage());
        }
    }

    // --- CASE 03: Đăng nhập tài khoản User vừa tạo ---
    private static void testLoginSuccess(String username, String password) {
        logAction("USER-LOGIN", String.format("TC_AUT02: Tiến hành ĐĂNG NHẬP tài khoản [%s]...", username));
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            LoginRequest loginReq = new LoginRequest(username, password);
            out.writeObject(loginReq);
            out.flush();

            Object response = in.readObject();
            if (response instanceof LoginResponse) {
                describeLoginResponse((LoginResponse) response, username);
            } else {
                logError("USER-LOGIN", "Nhận sai kiểu Object Response từ Server!");
            }

        } catch (Exception e) {
            logError("USER-LOGIN", "Lỗi kết nối Server: " + e.getMessage());
        }
    }

    // --- CASE 04: Đăng nhập bằng tài khoản ADMIN duy nhất ---
    private static void testAdminLoginSuccess() {
        logAction("ADMIN-LOGIN", String.format("TC_AUT03: Tiến hành ĐĂNG NHẬP bằng hệ thống ADMIN duy nhất [%s]...", ADMIN_USER));
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            LoginRequest loginReq = new LoginRequest(ADMIN_USER, ADMIN_PASS);
            out.writeObject(loginReq);
            out.flush();

            Object response = in.readObject();
            if (response instanceof LoginResponse) {
                describeLoginResponse((LoginResponse) response, ADMIN_USER);
            } else {
                logError("ADMIN-LOGIN", "Nhận sai kiểu Object Response từ Server!");
            }

        } catch (Exception e) {
            logError("ADMIN-LOGIN", "Lỗi kết nối Server: " + e.getMessage());
        }
    }

    // =========================================================================
    // HÀM ĐỊNH DẠNG LOG VÀ HIỂN THỊ (LOGGING UTILS)
    // =========================================================================
    private static void describeSignupResponse(SignupResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> SIGNUP] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeLoginResponse(LoginResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> LOGIN] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        if (res.isSuccess() && res.getUser() != null) {
            sb.append("   Quyền hạn : ").append(res.getUser().isAdmin() ? "👑 QUẢN TRỊ VIÊN (ADMIN)" : "👤 NGƯỜI DÙNG THƯỜNG").append("\n");
            sb.append("   Số dư ví  : ").append(res.getUser().getBalance()).append(" VND\n");
        }
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void logAction(String actor, String action) {
        synchronized (consoleLock) {
            System.out.println(String.format("👤 [%s] %s", actor, action));
        }
    }

    private static void logSystem(String message) {
        synchronized (consoleLock) {
            System.out.println(String.format("🚀 [HỆ THỐNG] %s", message));
        }
    }

    private static void logError(String actor, String error) {
        synchronized (consoleLock) {
            System.err.println(String.format("❌ [%s] LỖI: %s", actor, error));
        }
    }

    private static void printLog(String fullText) {
        synchronized (consoleLock) {
            System.out.print(fullText);
        }
    }
}