package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.event.AuctionCancelledEvent;
import com.groupproject.shared.network.event.AuctionEndedEvent;
import com.groupproject.shared.network.event.AuctionFinishedEvent;
import com.groupproject.shared.network.event.AuctionStartedEvent;
import com.groupproject.shared.network.event.NewBidEvent;
import com.groupproject.shared.network.event.SystemNotificationEvent;
import com.groupproject.shared.network.request.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.request.CreateAuctionRequest;
import com.groupproject.shared.network.request.GetMyAuctionsRequest;
import com.groupproject.shared.network.request.JoinAuctionRoomRequest;
import com.groupproject.shared.network.request.LeaveAuctionRoomRequest;
import com.groupproject.shared.network.request.LoginRequest;
import com.groupproject.shared.network.request.PlaceBidRequest;
import com.groupproject.shared.network.request.SignupRequest;
import com.groupproject.shared.network.request.TopUpRequest;
import com.groupproject.shared.network.response.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.response.CreateAuctionResponse;
import com.groupproject.shared.network.response.ErrorNotLoginResponse;
import com.groupproject.shared.network.response.GetMyAuctionsResponse;
import com.groupproject.shared.network.response.JoinAuctionRoomResponse;
import com.groupproject.shared.network.response.LeaveAuctionRoomResponse;
import com.groupproject.shared.network.response.LoginResponse;
import com.groupproject.shared.network.response.PlaceBidResponse;
import com.groupproject.shared.network.response.SignupResponse;
import com.groupproject.shared.network.response.TopUpResponse;

public class SystemSimulationTest {

    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";

    // Khóa đồng bộ màn hình Console để log không bị đè trộn lẫn nhau
    private static final Object consoleLock = new Object();

    // Biến lưu giữ ID phòng đấu giá do Seller tạo ra để truyền qua các luồng test
    private static volatile int sharedAuctionId = -1;
    
    // Sử dụng Latch để đồng bộ tiến trình của các Thread
    private static final CountDownLatch sharedAuctionIdLatch = new CountDownLatch(1);
    private static final CountDownLatch auctionCreatedLatch = new CountDownLatch(1);
    private static final CountDownLatch auctionActivatedLatch = new CountDownLatch(1);
    private static final CountDownLatch testCompletedLatch = new CountDownLatch(4); // 4 Luồng (Admin, Seller, BuyerA, BuyerB)

    public static void main(String[] args) {
        logSystem("=== KHỞI CHẠY HỆ THỐNG MÔ PHỎNG & TEST ĐA LUỒNG TOÀN DIỆN ===");

        // Chạy 4 luồng song song đại diện cho 4 người dùng thực tế
        new Thread(SystemSimulationTest::runAdminScenario, "Admin-Thread").start();
        new Thread(SystemSimulationTest::runSellerScenario, "Seller-Thread").start();
        new Thread(SystemSimulationTest::runBuyerAScenario, "BuyerA-Thread").start();
        new Thread(SystemSimulationTest::runBuyerBScenario, "BuyerB-Thread").start();

        try {
            // Chờ tối đa 60 giây để toàn bộ kịch bản chạy xong
            if (testCompletedLatch.await(60, TimeUnit.SECONDS)) {
                logSystem("=== HỆ THỐNG TEST ĐÃ HOÀN THÀNH TẤT CẢ KỊCH BẢN THÀNH CÔNG ===");
            } else {
                logWarning("SYSTEM", "Thời gian chạy test vượt quá giới hạn (Timeout). Tự động đóng tiến trình.");
            }
        } catch (InterruptedException e) {
            logError("SYSTEM", "Luồng chính bị ngắt quãng: " + e.getMessage());
        }
    }

    // =========================================================================
    // 1. KỊCH BẢN ADMIN (Chỉ giám sát)
    // =========================================================================
    private static void runAdminScenario() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "ADMIN");

            logAction("ADMIN", "Đang gửi yêu cầu ĐĂNG NHẬP...");
            LoginRequest loginReq = new LoginRequest("admin", "admin123");
            out.writeObject(loginReq);
            out.flush();

            // Admin chỉ ngồi yên giám sát hệ thống trong suốt 20 giây test
            Thread.sleep(20000);

        } catch (Exception e) {
            logError("ADMIN", "Xảy ra ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 2. KỊCH BẢN SELLER (Người Bán)
    // =========================================================================
    private static void runSellerScenario() {
        String sellerUser = "seller_" + (System.currentTimeMillis() % 1000);
        String pass = "pass123";

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "SELLER");

            // --- BƯỚC 1: KIỂM TRA BẢO MẬT (Gửi request khi chưa Đăng nhập) ---
            logAction("SELLER", "KIỂM TRA BẢO MẬT: Thử lấy danh sách cá nhân khi chưa ĐĂNG NHẬP...");
            out.writeObject(new GetMyAuctionsRequest());
            out.flush();
            Thread.sleep(1500);

            // --- BƯỚC 2: ĐĂNG KÝ MỚI & VÀO HỆ THỐNG ---
            logAction("SELLER", "Đang tiến hành ĐĂNG KÝ tài khoản người bán...");
            SignupRequest signupReq = new SignupRequest(sellerUser, sellerUser + "@auction.com", pass, pass);
            out.writeObject(signupReq);
            out.flush();
            Thread.sleep(1500);

            // --- BƯỚC 3: TẠO PHIÊN ĐẤU GIÁ MỚI ---
            logAction("SELLER", "Đang tạo một phiên đấu giá mới ở trạng thái WAITING...");
            
            Category dummyCategory = new Category(1, "Electronics", null);

            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "Mô hình Iron Man 1:1 Cực Hiếm",
                "Mô hình tỉ lệ thực tế có đèn LED toàn thân.",
                dummyCategory, null, 500.0, null, 
                java.time.LocalDateTime.now().plusMinutes(10).toString(), 
                AuctionStatus.WAITING
            );
            out.writeObject(createReq);
            out.flush();

            // Chờ Listener bắt được ID mới từ Server trả về (Timeout 5 giây)
            if (!sharedAuctionIdLatch.await(5, TimeUnit.SECONDS)) {
                logError("SELLER", "Không nhận được ID đấu giá mới tạo!");
                return;
            }
            auctionCreatedLatch.countDown(); // Báo cho Buyer biết đã có phòng

            Thread.sleep(1000);

            // --- BƯỚC 4: KIỂM TRA LẠI DANH SÁCH CỦA MÌNH ---
            logAction("SELLER", "Kiểm tra danh sách My Auctions (Lúc này CHẮC CHẮN PHẢI CÓ 1 PHIÊN)...");
            out.writeObject(new GetMyAuctionsRequest());
            out.flush();
            Thread.sleep(1500);

            // --- BƯỚC 5: KÍCH HOẠT PHIÊN ---
            logAction("SELLER", "Tiến hành kích hoạt phòng (ACTIVED) để khách vào đặt giá!");
            ChangeAuctionStatusRequest changeReq = new ChangeAuctionStatusRequest(sharedAuctionId, AuctionStatus.ACTIVED);
            out.writeObject(changeReq);
            out.flush();

            auctionActivatedLatch.countDown(); // Báo cho Buyer biết đã có thể đặt giá

            // Ngồi xem khách đấu giá trong 10 giây
            Thread.sleep(10000);

            // --- BƯỚC 6: HỦY PHIÊN ---
            logAction("SELLER", "Người bán tự quyết định HỦY phiên đấu giá...");
            ChangeAuctionStatusRequest cancelReq = new ChangeAuctionStatusRequest(sharedAuctionId, AuctionStatus.CANCELLED);
            out.writeObject(cancelReq);
            out.flush();

            Thread.sleep(2000); // Đợi hệ thống dọn dẹp

        } catch (Exception e) {
            logError("SELLER", "Xảy ra ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 3. KỊCH BẢN BUYER A (Người Mua 1)
    // =========================================================================
    private static void runBuyerAScenario() {
        String userA = "buyer_A_" + (System.currentTimeMillis() % 1000);
        String pass = "pass123";

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "BUYER-A");

            logAction("BUYER-A", "Đang tiến hành ĐĂNG KÝ...");
            SignupRequest signupReq = new SignupRequest(userA, userA + "@buyer.com", pass, pass);
            out.writeObject(signupReq);
            out.flush();

            // Chờ Seller tạo phòng xong
            auctionCreatedLatch.await();

            logAction("BUYER-A", "Vào phòng chờ (WAITING) ID: " + sharedAuctionId);
            out.writeObject(new JoinAuctionRoomRequest(sharedAuctionId));
            out.flush();

            logAction("BUYER-A", "Đang nạp 2000.0 VND vào tài khoản...");
            out.writeObject(new TopUpRequest(2000.0));
            out.flush();

            // Chờ Seller kích hoạt phòng
            auctionActivatedLatch.await();
            Thread.sleep(1000); // Tránh Bid quá nhanh khi phòng vừa mở

            logAction("BUYER-A", "Đặt mức giá khởi động: 600.0 VND...");
            out.writeObject(new PlaceBidRequest(sharedAuctionId, 600.0));
            out.flush();
            
            Thread.sleep(3000); // Chờ Buyer B phản đòn

            logAction("BUYER-A", "Bị đè giá! Đặt quyết liệt: 800.0 VND...");
            out.writeObject(new PlaceBidRequest(sharedAuctionId, 800.0));
            out.flush();
            
            Thread.sleep(8000); // Chờ xem kết quả bị hủy

        } catch (Exception e) {
            logError("BUYER-A", "Xảy ra ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 4. KỊCH BẢN BUYER B (Người Mua 2)
    // =========================================================================
    private static void runBuyerBScenario() {
        String userB = "buyer_B_" + ((System.currentTimeMillis() + 100) % 1000);
        String pass = "pass123";

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            startBackgroundListener(in, "BUYER-B");

            logAction("BUYER-B", "Đang tiến hành ĐĂNG KÝ...");
            SignupRequest signupReq = new SignupRequest(userB, userB + "@buyer.com", pass, pass);
            out.writeObject(signupReq);
            out.flush();

            // THÊM MỚI: Nạp tiền ngay sau khi đăng ký
            logAction("BUYER-B", "Đang nạp 2000.0 VND vào tài khoản...");
            out.writeObject(new TopUpRequest(2000.0));
            out.flush();

            // Chờ Seller kích hoạt phòng luôn rồi mới nhảy vào
            auctionActivatedLatch.await();

            logAction("BUYER-B", "Thấy phòng mở cửa (ACTIVED), nhảy vào ngay!");
            out.writeObject(new JoinAuctionRoomRequest(sharedAuctionId));
            out.flush();
            Thread.sleep(2000); // Đợi A đặt 600.0 trước

            logAction("BUYER-B", "Thấy A đặt 600.0, đè giá: 750.0 VND...");
            out.writeObject(new PlaceBidRequest(sharedAuctionId, 750.0));
            out.flush();

            Thread.sleep(3000); // Đợi A đặt 800.0

            logAction("BUYER-B", "Thử đặt giá láo (700.0) xem có bị chặn không...");
            out.writeObject(new PlaceBidRequest(sharedAuctionId, 700.0));
            out.flush();

            logAction("BUYER-B", "Chán, rời phòng đi tìm phòng khác!");
            out.writeObject(new LeaveAuctionRoomRequest(sharedAuctionId));
            out.flush();

            Thread.sleep(5000); // Chờ kịch bản đóng

        } catch (Exception e) {
            logError("BUYER-B", "Xảy ra ngoại lệ: " + e.getMessage());
        } finally {
            testCompletedLatch.countDown();
        }
    }

    // =========================================================================
    // 5. LUỒNG LẮNG NGHE CHỦ ĐỘNG (REAL-TIME LISTENER THREAD)
    // =========================================================================
    private static void startBackgroundListener(ObjectInputStream in, String threadRole) {
        new Thread(() -> {
            try {
                while (true) {
                    Object receivedObj = in.readObject();
                    if (receivedObj == null) break;

                    // --- PHÂN LOẠI ĐỂ IN PHẢN HỒI RESPONSE ---
                    if (receivedObj instanceof SignupResponse) {
                        describeSignupResponse((SignupResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof LoginResponse) {
                        describeLoginResponse((LoginResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof CreateAuctionResponse) {
                        CreateAuctionResponse res = (CreateAuctionResponse) receivedObj;
                        if (res.isSuccess() && res.getAuction() != null) {
                            sharedAuctionId = res.getAuction().getId();
                            sharedAuctionIdLatch.countDown(); // Báo hiệu đã lấy được ID
                        }
                        describeCreateResponse(res, threadRole);
                    } else if (receivedObj instanceof ChangeAuctionStatusResponse) {
                        describeChangeStatusResponse((ChangeAuctionStatusResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof GetMyAuctionsResponse) {
                        describeGetMyAuctionsResponse((GetMyAuctionsResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof JoinAuctionRoomResponse) {
                        describeJoinRoomResponse((JoinAuctionRoomResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof LeaveAuctionRoomResponse) {
                        describeLeaveRoomResponse((LeaveAuctionRoomResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof PlaceBidResponse) {
                        describePlaceBidResponse((PlaceBidResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof ErrorNotLoginResponse) {
                        describeErrorNotLoginResponse((ErrorNotLoginResponse) receivedObj, threadRole);
                    }
                    
                    // --- PHÂN LOẠI ĐỂ IN EVENT CHỦ ĐỘNG TỪ SERVER (REAL-TIME BROADCASTS) ---
                    else if (receivedObj instanceof TopUpResponse) {
                        describeTopUpResponse((TopUpResponse) receivedObj, threadRole);
                    } else if (receivedObj instanceof NewBidEvent) {
                        NewBidEvent event = (NewBidEvent) receivedObj;
                        logNotification(threadRole, String.format("📢 [EVENT] Phòng #%d vừa có mức giá mới: %.2f VND!", 
                            event.getAuctionId(), event.getNewHighestBid()));
                    } else if (receivedObj instanceof AuctionStartedEvent) {
                        AuctionStartedEvent event = (AuctionStartedEvent) receivedObj;
                        logNotification(threadRole, String.format("🟢 [EVENT] Phòng #%d chính thức MỞ CỬA!", event.getAuctionId()));
                    } else if (receivedObj instanceof AuctionFinishedEvent) {
                        AuctionFinishedEvent event = (AuctionFinishedEvent) receivedObj;
                        logNotification(threadRole, String.format("🟡 [EVENT] Phòng #%d hết giờ! Đang xử lý giao dịch...", event.getAuctionId()));
                    } else if (receivedObj instanceof AuctionEndedEvent) {
                        AuctionEndedEvent event = (AuctionEndedEvent) receivedObj;
                        logNotification(threadRole, String.format("🏆 [EVENT] Phiên #%d hoàn tất! Winner: %d - Giá chốt: %.2f VND!", 
                            event.getAuctionId(), event.getWinnerId(), event.getFinalPrice()));
                    } else if (receivedObj instanceof AuctionCancelledEvent) {
                        AuctionCancelledEvent event = (AuctionCancelledEvent) receivedObj;
                        logNotification(threadRole, String.format("🛑 [EVENT] PHIÊN #%d BỊ HỦY! Lý do: %s", 
                            event.getAuctionId(), event.getReason()));
                    } else if (receivedObj instanceof SystemNotificationEvent) {
                        SystemNotificationEvent event = (SystemNotificationEvent) receivedObj;
                        logNotification(threadRole, String.format("💬 [SYSTEM-LOG] %s", event.toString()));
                    }
                }
            } catch (Exception e) {
                logSystem(String.format("Luồng lắng nghe của [%s] ngắt kết nối.", threadRole));
            }
        }, "Listener-" + threadRole).start();
    }

    // =========================================================================
    // 6. CÁC HÀM TRÌNH BÀY PHẢN HỒI (SYNCHRONIZED CONSOLE LOGS)
    // =========================================================================

    private static void describeSignupResponse(SignupResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> SIGNUP] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        if (res.isSuccess() && res.getUser() != null) {
            sb.append("   User mới  : ID=").append(res.getUser().getId()).append("\n");
        }
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeLoginResponse(LoginResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> LOGIN] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        if (res.isSuccess() && res.getUser() != null) {
            sb.append("   Chào mừng : ").append(res.getUser().getUsername()).append("\n");
        }
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeCreateResponse(CreateAuctionResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> CREATE AUCTION] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        if (res.isSuccess() && res.getAuction() != null) {
            sb.append("   ID Phòng  : ").append(res.getAuction().getId()).append("\n");
        }
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeChangeStatusResponse(ChangeAuctionStatusResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> CHANGE STATUS] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ ĐÃ THAY ĐỔI" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n"); // Đã bổ sung in lỗi để truy vết
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeGetMyAuctionsResponse(GetMyAuctionsResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> MY AUCTIONS] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        if (res.isSuccess() && res.getMyAuctions() != null) {
            sb.append("   Tổng số   : ").append(res.getMyAuctions().size()).append(" đấu giá do tôi sở hữu.\n");
        }
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeJoinRoomResponse(JoinAuctionRoomResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> JOIN ROOM] (%s)\n", actor));
        sb.append("   Kết quả   : ").append(res.isSuccess() ? "✅ VÀO PHÒNG" : "❌ TỪ CHỐI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeLeaveRoomResponse(LeaveAuctionRoomResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> LEAVE ROOM] (%s)\n", actor));
        sb.append("   Kết quả   : ").append(res.isSuccess() ? "✅ ĐÃ RỜI PHÒNG" : "❌ LỖI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describePlaceBidResponse(PlaceBidResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> PLACE BID] (%s)\n", actor));
        sb.append("   Ghi nhận  : ").append(res.isSuccess() ? "✅ CHẤP NHẬN GIÁ" : "❌ BỊ TỪ CHỐI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeErrorNotLoginResponse(ErrorNotLoginResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n🛡️ [SECURITY -> CHẶN TRUY CẬP] (%s)\n", actor));
        sb.append("   Cảnh báo  : ").append(res.getErrorMessage()).append("\n");
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeTopUpResponse(TopUpResponse res, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> TOP UP] (%s)\n", actor));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        if (res.isSuccess()) {
            sb.append("   Số dư mới : ").append(res.getNewBalance()).append(" VND\n");
        }
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