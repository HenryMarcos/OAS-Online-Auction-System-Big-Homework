package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.event.AuctionCancelledEvent;
import com.groupproject.shared.network.event.AuctionStartedEvent;
import com.groupproject.shared.network.event.NewBidEvent;
import com.groupproject.shared.network.event.SystemNotificationEvent;
import com.groupproject.shared.network.request.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.request.CreateAuctionRequest;
import com.groupproject.shared.network.request.JoinAuctionRoomRequest;
import com.groupproject.shared.network.request.LoginRequest;
import com.groupproject.shared.network.request.PlaceBidRequest;
import com.groupproject.shared.network.request.SignupRequest;
import com.groupproject.shared.network.request.TopUpRequest;
import com.groupproject.shared.network.response.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.response.CreateAuctionResponse;
import com.groupproject.shared.network.response.JoinAuctionRoomResponse;
import com.groupproject.shared.network.response.PlaceBidResponse;

/**
 * Test Suite 07: Kịch bản Mô phỏng Toàn diện (CrossFunctionTest)
 * Hành trình: Signup -> Tạo phòng -> Kích hoạt -> Cạnh tranh giá -> Admin Hủy phòng -> Mọi người nhận thông báo.
 */
public class CrossFunctionTest {

    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final Object consoleLock = new Object();

    // Biến lưu giữ ID phòng đấu giá
    private static volatile int sharedAuctionId = -1;
    
    // Hệ thống "Đèn giao thông" để các luồng điều phối nhau chạy nhịp nhàng
    private static final CountDownLatch auctionCreatedLatch = new CountDownLatch(1);
    private static final CountDownLatch auctionActivatedLatch = new CountDownLatch(1);
    private static final CountDownLatch buyerABidDoneLatch = new CountDownLatch(1);
    private static final CountDownLatch buyerBBidDoneLatch = new CountDownLatch(1);
    private static final CountDownLatch adminCancelledLatch = new CountDownLatch(1);
    
    private static final CountDownLatch testCompletedLatch = new CountDownLatch(4);

    public static void main(String[] args) {
        logSystem("=== 🎬 KHỞI CHẠY MÔ PHỎNG END-TO-END (CROSS_FUNCTION_TEST) ===");
        logSystem("Kịch bản: Seller tạo phòng -> Buyer tranh giá -> Admin xử lý vi phạm hủy phòng.");

        new Thread(CrossFunctionTest::runAdminScenario, "Admin-Thread").start();
        new Thread(CrossFunctionTest::runSellerScenario, "Seller-Thread").start();
        new Thread(CrossFunctionTest::runBuyerAScenario, "BuyerA-Thread").start();
        new Thread(CrossFunctionTest::runBuyerBScenario, "BuyerB-Thread").start();

        try {
            if (testCompletedLatch.await(45, TimeUnit.SECONDS)) {
                logSystem("=== 🏁 MÔ PHỎNG HOÀN TẤT THÀNH CÔNG ===");
            } else {
                logWarning("SYSTEM", "Mô phỏng vượt quá thời gian (Timeout 45s). Đóng tiến trình.");
            }
        } catch (InterruptedException e) {
            logError("SYSTEM", "Luồng chính bị ngắt quãng.");
        }
    }

    // =========================================================================
    // 1. KỊCH BẢN SELLER (Tạo & Mở phòng)
    // =========================================================================
    private static void runSellerScenario() {
        String sellerUser = "seller_sim_" + System.currentTimeMillis();
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "SELLER");

            // Đăng ký & Đăng nhập
            out.writeObject(new SignupRequest(sellerUser, sellerUser + "@test.com", "pass123", "pass123")); out.flush();
            Thread.sleep(500);
            out.writeObject(new LoginRequest(sellerUser, "pass123")); out.flush();
            Thread.sleep(500);

            // Tạo phòng WAITING
            logAction("SELLER", "Tạo phòng đấu giá mới (WAITING)...");
            Category cat = new Category(1, "Electronics", null);
            out.writeObject(new CreateAuctionRequest("Laptop Gaming Alienware", "Hàng lướt", cat, null, 1500.0, null, 
                                 LocalDateTime.now().plusHours(1).toString(), AuctionStatus.WAITING));
            out.flush();

            // Đợi listener lấy được ID
            auctionCreatedLatch.await();
            Thread.sleep(1000);

            // Kích hoạt phòng
            logAction("SELLER", "Kích hoạt phòng (ACTIVED) cho khách vào!");
            out.writeObject(new ChangeAuctionStatusRequest(sharedAuctionId, AuctionStatus.ACTIVED));
            out.flush();

            // Đợi listener xác nhận ACTIVED
            auctionActivatedLatch.await();

            // Ngồi chờ Admin vung búa
            adminCancelledLatch.await();
            Thread.sleep(2000); // Đợi đọc nốt các log sự kiện

        } catch (Exception e) {
            logError("SELLER", "Ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 2. KỊCH BẢN BUYER A (Đặt giá đầu tiên)
    // =========================================================================
    private static void runBuyerAScenario() {
        String buyerA = "buyer_A_" + System.currentTimeMillis();
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "BUYER-A");

            // Đăng ký, Đăng nhập, Nạp tiền
            out.writeObject(new SignupRequest(buyerA, buyerA + "@test.com", "pass123", "pass123")); out.flush();
            Thread.sleep(500);
            out.writeObject(new LoginRequest(buyerA, "pass123")); out.flush();
            Thread.sleep(500);
            out.writeObject(new TopUpRequest(5000.0)); out.flush();
            Thread.sleep(500);

            // Chờ phòng mở cửa
            auctionActivatedLatch.await();
            
            logAction("BUYER-A", "Vào phòng ID: " + sharedAuctionId);
            out.writeObject(new JoinAuctionRoomRequest(sharedAuctionId)); out.flush();
            Thread.sleep(1000);

            logAction("BUYER-A", "Đặt giá mở bát: 1800.0 VND");
            out.writeObject(new PlaceBidRequest(sharedAuctionId, 1800.0)); out.flush();
            
            // Báo hiệu đã đặt giá xong
            buyerABidDoneLatch.countDown();

            // Chờ admin hủy phòng
            adminCancelledLatch.await();
            Thread.sleep(2000);

        } catch (Exception e) {
            logError("BUYER-A", "Ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 3. KỊCH BẢN BUYER B (Kẻ đè giá)
    // =========================================================================
    private static void runBuyerBScenario() {
        String buyerB = "buyer_B_" + (System.currentTimeMillis() + 10);
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "BUYER-B");

            out.writeObject(new SignupRequest(buyerB, buyerB + "@test.com", "pass123", "pass123")); out.flush();
            Thread.sleep(500);
            out.writeObject(new LoginRequest(buyerB, "pass123")); out.flush();
            Thread.sleep(500);
            out.writeObject(new TopUpRequest(10000.0)); out.flush();
            
            // Chờ Buyer A đặt giá xong mới nhảy vào để test Đồng bộ Late Joiner
            buyerABidDoneLatch.await();
            Thread.sleep(500);

            logAction("BUYER-B", "Vào muộn, check giá phòng ID: " + sharedAuctionId);
            out.writeObject(new JoinAuctionRoomRequest(sharedAuctionId)); out.flush();
            Thread.sleep(1000);

            logAction("BUYER-B", "Đè giá Buyer A. Đặt: 2500.0 VND");
            out.writeObject(new PlaceBidRequest(sharedAuctionId, 2500.0)); out.flush();

            // Báo hiệu B đã đặt xong
            buyerBBidDoneLatch.countDown();

            // Chờ admin hủy phòng
            adminCancelledLatch.await();
            Thread.sleep(2000);

        } catch (Exception e) {
            logError("BUYER-B", "Ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 4. KỊCH BẢN ADMIN (Giám sát & Xử phạt)
    // =========================================================================
    private static void runAdminScenario() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "ADMIN");

            out.writeObject(new LoginRequest("admin", "admin123")); out.flush();
            
            // Admin đợi Buyer B đặt giá xong (phòng đang cao trào)
            buyerBBidDoneLatch.await();
            Thread.sleep(1500);

            logAction("ADMIN", "Phát hiện phòng đấu giá vi phạm quy chế. Tiến hành HỦY NGAY LẬP TỨC!");
            out.writeObject(new ChangeAuctionStatusRequest(sharedAuctionId, AuctionStatus.CANCELLED));
            out.flush();

            Thread.sleep(1000); // Đợi Server xử lý
            adminCancelledLatch.countDown(); // Báo hiệu đã vung búa

            Thread.sleep(2000);

        } catch (Exception e) {
            logError("ADMIN", "Ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 5. LISTENER LẮNG NGHE BACKGROUND
    // =========================================================================
    private static void startBackgroundListener(ObjectInputStream in, String role) {
        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj == null) break;

                    // --- XỬ LÝ RESPONSE VÀ ĐIỀU PHỐI ĐÈN GIAO THÔNG ---
                    if (obj instanceof CreateAuctionResponse) {
                        CreateAuctionResponse res = (CreateAuctionResponse) obj;
                        if (res.isSuccess() && res.getAuction() != null) {
                            sharedAuctionId = res.getAuction().getId();
                            auctionCreatedLatch.countDown();
                        }
                        describeCreateResponse(res, role);
                    } 
                    else if (obj instanceof ChangeAuctionStatusResponse) {
                        ChangeAuctionStatusResponse res = (ChangeAuctionStatusResponse) obj;
                        if (res.isSuccess() && res.getMessage().toLowerCase().contains("bắt đầu")) {
                            auctionActivatedLatch.countDown();
                        }
                        describeChangeStatusResponse(res, role);
                    }
                    else if (obj instanceof JoinAuctionRoomResponse) {
                        describeJoinRoomResponse((JoinAuctionRoomResponse) obj, role);
                    }
                    else if (obj instanceof PlaceBidResponse) {
                        describePlaceBidResponse((PlaceBidResponse) obj, role);
                    }
                    // Bỏ qua log Login/Signup/Topup để màn hình đỡ rối
                    
                    // --- XỬ LÝ EVENT REAL-TIME ---
                    else if (obj instanceof NewBidEvent) {
                        NewBidEvent e = (NewBidEvent) obj;
                        logNotification(role, String.format("📢 [EVENT] Phòng #%d nảy giá mới: %.2f VND!", e.getAuctionId(), e.getNewHighestBid()));
                    } 
                    else if (obj instanceof AuctionStartedEvent) {
                        logNotification(role, "🟢 [EVENT] Phòng đã MỞ CỬA!");
                    } 
                    else if (obj instanceof AuctionCancelledEvent) {
                        AuctionCancelledEvent e = (AuctionCancelledEvent) obj;
                        logNotification(role, "🛑 [EVENT] PHÒNG BỊ HỦY! Lý do: " + e.getReason());
                    } 
                    else if (obj instanceof SystemNotificationEvent) {
                        SystemNotificationEvent e = (SystemNotificationEvent) obj;
                        logNotification(role, String.format("💬 [SYSTEM-LOG] %s: %s", e.getSenderName(), e.getMessage()));
                    }
                }
            } catch (Exception ignored) {
                // Thoát im lặng khi Socket đóng
            }
        }, "Listen-" + role).start();
    }

    // =========================================================================
    // 6. GIAO DIỆN CONSOLE
    // =========================================================================
    private static void describeCreateResponse(CreateAuctionResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> CREATE] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅" : "❌").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeChangeStatusResponse(ChangeAuctionStatusResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> STATUS] (%s)\n", actor));
        sb.append("   Kết quả   : ").append(res.isSuccess() ? "✅" : "❌").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeJoinRoomResponse(JoinAuctionRoomResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> JOIN ROOM] (%s)\n", actor));
        sb.append("   Kết quả   : ").append(res.isSuccess() ? "✅" : "❌").append("\n");
        if (res.isSuccess() && res.getAuctionDetails() != null) {
            sb.append("   [SYNC] Giá hiện tại đang là: ").append(res.getAuctionDetails().getCurrentBid()).append(" VND\n");
        }
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describePlaceBidResponse(PlaceBidResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> BID] (%s)\n", actor));
        sb.append("   Kết quả   : ").append(res.isSuccess() ? "✅" : "❌").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void logAction(String actor, String action) {
        synchronized (consoleLock) { System.out.println(String.format("👤 [%s] %s", actor, action)); }
    }

    private static void logNotification(String actor, String eventText) {
        synchronized (consoleLock) { System.out.println(String.format("🔔 [%s] %s", actor, eventText)); }
    }

    private static void logSystem(String message) {
        synchronized (consoleLock) { System.out.println(String.format("🚀 [HỆ THỐNG] %s", message)); }
    }

    private static void logWarning(String actor, String warning) {
        synchronized (consoleLock) { System.out.println(String.format("⚠️ [%s] CẢNH BÁO: %s", actor, warning)); }
    }

    private static void logError(String actor, String error) {
        synchronized (consoleLock) { System.err.println(String.format("❌ [%s] LỖI: %s", actor, error)); }
    }

    private static void printLog(String fullText) {
        synchronized (consoleLock) { System.out.print(fullText); }
    }
}