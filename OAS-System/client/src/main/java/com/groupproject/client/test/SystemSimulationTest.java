package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.request.LoginRequest;
import com.groupproject.shared.network.request.SignupRequest;
import com.groupproject.shared.network.response.LoginResponse;
import com.groupproject.shared.network.response.SignupResponse;

public class SystemSimulationTest {

    private static final int SERVER_PORT = 8080; // Kiểm tra lại Config.SERVER_PORT của bạn
    private static final String SERVER_HOST = "localhost";

    public static void main(String[] args) {
        System.out.println("🚀 === KHỞI CHẠY HỆ THỐNG TEST TOÀN DIỆN (OAS-SYSTEM) ===");

        // Chạy song song 2 kịch bản
        new Thread(SystemSimulationTest::runAdminScenario, "Admin-Thread").start();
        new Thread(SystemSimulationTest::runUserScenario, "User-Thread").start();
    }

    private static void runUserScenario() {
        String randomSuffix = String.valueOf(System.currentTimeMillis() % 1000);
        String testUser = "buyer_" + randomSuffix;
        String testEmail = "buyer" + randomSuffix + "@example.com";
        String testPass = "pass123";

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            logAction("USER", "Đã kết nối. Đang gửi yêu cầu ĐĂNG KÝ...");

            // 1. TEST ĐĂNG KÝ (SignupRequest)
            SignupRequest sigReq = new SignupRequest(testUser, testEmail, testPass, testPass);
            out.writeObject(sigReq);
            out.flush();

            SignupResponse sigRes = (SignupResponse) in.readObject();
            describeSignupResponse(sigRes);

            if (!sigRes.isSuccess()) return;

            Thread.sleep(1500); // Đợi một chút cho giống người thật

            // 2. TEST ĐĂNG NHẬP (LoginRequest)
            logAction("USER", "Đang gửi yêu cầu ĐĂNG NHẬP...");
            LoginRequest logReq = new LoginRequest(testUser, testPass);
            out.writeObject(logReq);
            out.flush();

            LoginResponse logRes = (LoginResponse) in.readObject();
            describeLoginResponse(logRes);

        } catch (Exception e) {
            logError("USER", e.getMessage());
        }
    }

    private static void runAdminScenario() {
        // Admin thường đã có sẵn trong DB (Seed data), nên ta test đăng nhập thẳng
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            logAction("ADMIN", "Kết nối. Đang đăng nhập tài khoản Quản trị...");
            
            LoginRequest adminReq = new LoginRequest("admin", "admin123");
            out.writeObject(adminReq);
            out.flush();

            LoginResponse adminRes = (LoginResponse) in.readObject();
            describeLoginResponse(adminRes);

        } catch (Exception e) {
            logError("ADMIN", e.getMessage());
        }
    }

    // --- LOGGING HELPERS (In ra chi tiết Response bạn yêu cầu) ---

    private static void describeSignupResponse(SignupResponse res) {
        System.out.println("\n📩 [SERVER -> SIGNUP PHẢN HỒI]");
        System.out.println("   Trạng thái: " + (res.isSuccess() ? "✅ OK" : "❌ THẤT BẠI"));
        System.out.println("   Message   : " + res.getMessage());
        if (res.isSuccess() && res.getUser() != null) {
            System.out.println("   User mới  : ID=" + res.getUser().getId() + ", Is_Admin=" + res.getUser().isAdmin());
            System.out.println("   Dữ liệu   : Nhận được " + (res.getCategoryTree() != null ? res.getCategoryTree().size() : 0) + " danh mục.");
        }
        System.out.println("--------------------------------------------------\n");
    }

    private static void describeLoginResponse(LoginResponse res) {
        System.out.println("\n📩 [SERVER -> LOGIN PHẢN HỒI]");
        System.out.println("   Trạng thái: " + (res.isSuccess() ? "✅ OK" : "❌ THẤT BẠI"));
        System.out.println("   Message   : " + res.getMessage());
        if (res.isSuccess() && res.getUser() != null) {
            User u = res.getUser();
            System.out.println("   Welcome   : " + u.getUsername() + " (Số dư: " + u.getBalance() + ")");
            System.out.println("   Data      : Nhận được " + (res.getAuctionList() != null ? res.getAuctionList().size() : 0) + " phiên đấu giá.");
        }
        System.out.println("--------------------------------------------------\n");
    }

    private static void logAction(String actor, String action) {
        System.out.println("👤 [" + actor + "] " + action);
    }

    private static void logError(String actor, String err) {
        System.err.println("❌ [" + actor + "] LỖI: " + err);
    }
}