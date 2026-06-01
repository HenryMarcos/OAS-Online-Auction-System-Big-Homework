package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.requests.JoinAuctionRoomRequest;
import com.groupproject.shared.network.requests.LeaveAuctionRoomRequest;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.PlaceBidRequest;
import com.groupproject.shared.network.requests.SignupRequest;
import com.groupproject.shared.network.requests.TopUpRequest;
import com.groupproject.shared.network.responses.CreateAuctionResponse;
import com.groupproject.shared.network.responses.JoinAuctionRoomResponse;
import com.groupproject.shared.network.responses.LoginResponse;
import com.groupproject.shared.network.responses.PlaceBidResponse;
import com.groupproject.shared.network.responses.SignupResponse;
import com.groupproject.shared.network.responses.TopUpResponse;

/**
 * Test Suite 05: Room Session & Pub/Sub Events (RoomSessionTest)
 * Tập trung: Đồng bộ giá cho người vào muộn, Broadcast Event và Hủy theo dõi khi rời phòng.
 */
public class RoomSessionTest {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final Object consoleLock = new Object();

    public static void main(String[] args) {
        logSystem("=== KHỞI CHẠY KIỂM THỬ SESSION PHÒNG ĐẤU GIÁ (ROOM_SESSION_TEST) ===");
        runPubSubScenario();
        logSystem("=== HỆ THỐNG KIỂM THỬ ROOM_SESSION_TEST HOÀN THÀNH ===");
    }

    private static void runPubSubScenario() {
        long ts = System.currentTimeMillis();
        String seller = "seller_rs_" + ts;
        String buyer1 = "buyer1_rs_" + ts;
        String buyer2 = "buyer2_rs_" + ts;

        try (
            Socket sockSeller = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream outSeller = new ObjectOutputStream(sockSeller.getOutputStream());
            ObjectInputStream inSeller = new ObjectInputStream(sockSeller.getInputStream());

            Socket sockB1 = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream outB1 = new ObjectOutputStream(sockB1.getOutputStream());
            ObjectInputStream inB1 = new ObjectInputStream(sockB1.getInputStream());

            Socket sockB2 = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream outB2 = new ObjectOutputStream(sockB2.getOutputStream());
            ObjectInputStream inB2 = new ObjectInputStream(sockB2.getInputStream())
        ) {
            // =================================================================
            // BƯỚC 1: CHUẨN BỊ VÀ TẠO PHÒNG
            // =================================================================
            logSystem("--- BƯỚC 1: Đăng ký, Nạp tiền & Mở phòng ---");
            setupSession(outSeller, inSeller, seller, "pass123");
            setupSession(outB1, inB1, buyer1, "pass123");
            setupSession(outB2, inB2, buyer2, "pass123");

            outB1.writeObject(new TopUpRequest(10000.0)); outB1.flush();
            readExpectedResponse(inB1, TopUpResponse.class);
            outB2.writeObject(new TopUpRequest(10000.0)); outB2.flush();
            readExpectedResponse(inB2, TopUpResponse.class);

            Category cat = new Category(1, "Jewelry", null);
            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "Nhẫn Kim Cương", "Giá khởi điểm 1000", cat, null, 1000.0, null, 
                LocalDateTime.now().plusHours(1).toString(), AuctionStatus.ACTIVED
            );
            outSeller.writeObject(createReq); outSeller.flush();
            CreateAuctionResponse createRes = readExpectedResponse(inSeller, CreateAuctionResponse.class);
            int auctionId = createRes.getAuction().getId();
            logAction(seller, "Đã tạo phòng ACTIVED ID: " + auctionId);

            // =================================================================
            // BƯỚC 2: BUYER 1 VÀO VÀ ĐẶT GIÁ LẦN 1
            // =================================================================
            logSystem("--- BƯỚC 2: Buyer 1 đặt giá để thay đổi mức giá hiện tại ---");
            outB1.writeObject(new JoinAuctionRoomRequest(auctionId)); outB1.flush();
            readExpectedResponse(inB1, JoinAuctionRoomResponse.class);

            outB1.writeObject(new PlaceBidRequest(auctionId, 1500.0)); outB1.flush();
            readExpectedResponse(inB1, PlaceBidResponse.class);
            logAction(buyer1, "Đã đặt giá lên 1500.0 VND.");

            // =================================================================
            // BƯỚC 3: TEST "LATE JOINER" - NGƯỜI VÀO SAU ĐƯỢC ĐỒNG BỘ GIÁ TỐT NHẤT
            // =================================================================
            logSystem("--- BƯỚC 3: Test đồng bộ dữ liệu cho người vào muộn (Late Joiner) ---");
            outB2.writeObject(new JoinAuctionRoomRequest(auctionId)); outB2.flush();
            JoinAuctionRoomResponse joinResB2 = readExpectedResponse(inB2, JoinAuctionRoomResponse.class);
            
            if (joinResB2.isSuccess() && joinResB2.getAuctionDetails() != null) {
                double syncedPrice = joinResB2.getAuctionDetails().getCurrentBid();
                logAction(buyer2, "Vừa vào phòng. Giá hiện tại thấy được: " + syncedPrice + " VND");
                if (syncedPrice == 1500.0) {
                    logSystem("✅ ĐÚNG: Hệ thống đã đồng bộ giá mới nhất (1500.0) cho người vào muộn.");
                } else {
                    logError("SYNC", "❌ LỖI: Giá hiện tại lẽ ra phải là 1500.0 nhưng lại là " + syncedPrice);
                }
            }

            // =================================================================
            // BƯỚC 4: TEST BROADCAST EVENT - NGƯỜI ĐANG TRONG PHÒNG NHẬN ĐƯỢC THÔNG BÁO
            // =================================================================
            logSystem("--- BƯỚC 4: Test luồng Broadcast Event (Pub/Sub) ---");
            logAction(buyer2, "Tiến hành đặt giá 2000.0 VND...");
            outB2.writeObject(new PlaceBidRequest(auctionId, 2000.0)); outB2.flush();
            readExpectedResponse(inB2, PlaceBidResponse.class);

            // B1 đang trong phòng, B1 bắt buộc phải nhận được NewBidEvent
            Object eventObj = inB1.readObject();
            if (eventObj instanceof NewBidEvent) {
                logAction(buyer1, "Đã nhận được NewBidEvent real-time từ hệ thống!");
                logSystem("✅ ĐÚNG: Cơ chế Pub/Sub hoạt động trơn tru trong phòng.");
            } else {
                logError(buyer1, "❌ LỖI: Nhận được Object lạ, không phải NewBidEvent. " + eventObj.getClass().getSimpleName());
            }

            // =================================================================
            // BƯỚC 5: TEST LEAVE ROOM - RỜI PHÒNG THÌ PHẢI BỊ NGẮT EVENT
            // =================================================================
            logSystem("--- BƯỚC 5: Test cắt liên kết Event khi rời phòng ---");
            outB1.writeObject(new LeaveAuctionRoomRequest(auctionId)); outB1.flush();
            // Đọc response phản hồi (nếu thiết kế của bạn có trả về response cho việc rời phòng)
            // readExpectedResponse(inB1, Response.class); 
            logAction(buyer1, "Đã gửi yêu cầu THOÁT KHỎI PHÒNG.");

            logAction(buyer2, "Tiếp tục đặt giá 2500.0 VND...");
            outB2.writeObject(new PlaceBidRequest(auctionId, 2500.0)); outB2.flush();
            readExpectedResponse(inB2, PlaceBidResponse.class);

            // Set timeout 2 giây cho B1. Nếu quá 2 giây không nhận được gì -> Hệ thống chuẩn (không bị rác event).
            sockB1.setSoTimeout(2000);
            try {
                Object strayObj = inB1.readObject();
                // NẾU XUỐNG ĐƯỢC DÒNG NÀY TỨC LÀ LỖI (VẪN NHẬN ĐƯỢC TIN NHẮN)
                if (strayObj instanceof NewBidEvent) {
                    logError("ISOLATION", "❌ LỖI NGHIÊM TRỌNG: Buyer 1 đã rời phòng nhưng VẪN nhận được NewBidEvent!");
                }
            } catch (SocketTimeoutException timeout) {
                logSystem("✅ ĐÚNG: Socket chờ 2 giây không nhận được gì. Buyer 1 đã được ngắt Event thành công!");
            }
            // Trả lại timeout mặc định (vô hạn) để không ảnh hưởng các test sau này (nếu có)
            sockB1.setSoTimeout(0);

        } catch (Exception e) {
            logError("ROOM_SESSION", "Xảy ra ngoại lệ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH
    // =========================================================================

    /**
     * Chỉ đọc đúng Response, bỏ qua mọi Event xen ngang.
     */
    @SuppressWarnings("unchecked")
    private static <T> T readExpectedResponse(ObjectInputStream in, Class<T> expectedClass) throws Exception {
        while (true) {
            Object obj = in.readObject();
            if (expectedClass.isInstance(obj)) {
                return expectedClass.cast(obj);
            }
        }
    }

    private static void setupSession(ObjectOutputStream out, ObjectInputStream in, String user, String pass) throws Exception {
        out.writeObject(new SignupRequest(user, user + "@test.com", pass, pass)); out.flush();
        readExpectedResponse(in, SignupResponse.class);
        out.writeObject(new LoginRequest(user, pass)); out.flush();
        readExpectedResponse(in, LoginResponse.class);
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