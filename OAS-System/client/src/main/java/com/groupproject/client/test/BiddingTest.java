package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
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
import com.groupproject.shared.network.response.LoginResponse;
import com.groupproject.shared.network.response.PlaceBidResponse;
import com.groupproject.shared.network.response.SignupResponse;
import com.groupproject.shared.network.response.TopUpResponse;

/**
 * Test Suite 04: Bidding Logic (BiddingTest)
 * Tập trung: Đặt giá hợp lệ, Chặn giá thấp, Chặn thiếu tiền, Chặn đặt giá khi phòng đóng.
 */
public class BiddingTest {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final Object consoleLock = new Object();

    public static void main(String[] args) {
        logSystem("=== KHỞI CHẠY KIỂM THỬ NGHIỆP VỤ ĐẶT GIÁ (BIDDING_TEST) ===");
        runBiddingScenario();
        logSystem("=== HỆ THỐNG KIỂM THỬ BIDDING_TEST HOÀN THÀNH ===");
    }

    private static void runBiddingScenario() {
        long ts = System.currentTimeMillis();
        String seller = "seller_" + ts;
        String buyer1 = "buyer1_" + ts;
        String buyer2 = "buyer2_" + ts;
        String buyer3 = "buyer3_" + ts;

        try (
            // Khởi tạo 4 luồng mạng độc lập mô phỏng 4 máy tính khác nhau
            Socket sockSeller = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream outSeller = new ObjectOutputStream(sockSeller.getOutputStream());
            ObjectInputStream inSeller = new ObjectInputStream(sockSeller.getInputStream());

            Socket sockB1 = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream outB1 = new ObjectOutputStream(sockB1.getOutputStream());
            ObjectInputStream inB1 = new ObjectInputStream(sockB1.getInputStream());

            Socket sockB2 = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream outB2 = new ObjectOutputStream(sockB2.getOutputStream());
            ObjectInputStream inB2 = new ObjectInputStream(sockB2.getInputStream());

            Socket sockB3 = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream outB3 = new ObjectOutputStream(sockB3.getOutputStream());
            ObjectInputStream inB3 = new ObjectInputStream(sockB3.getInputStream())
        ) {
            // =================================================================
            // BƯỚC 1: TIỀN TRẠM - ĐĂNG KÝ, ĐĂNG NHẬP & NẠP TIỀN
            // =================================================================
            logSystem("--- BƯỚC 1: Chuẩn bị tài khoản ---");
            setupSession(outSeller, inSeller, seller, "pass123");
            setupSession(outB1, inB1, buyer1, "pass123");
            setupSession(outB2, inB2, buyer2, "pass123");
            setupSession(outB3, inB3, buyer3, "pass123");

            // B1 nạp 5000, B2 nạp 5000, B3 KHÔNG nạp tiền (0 VND)
            outB1.writeObject(new TopUpRequest(5000.0)); outB1.flush();
            readExpectedResponse(inB1, TopUpResponse.class);
            
            outB2.writeObject(new TopUpRequest(5000.0)); outB2.flush();
            readExpectedResponse(inB2, TopUpResponse.class);

            // =================================================================
            // BƯỚC 2: SELLER TẠO VÀ MỞ PHÒNG
            // =================================================================
            logSystem("--- BƯỚC 2: Seller tạo và mở phòng đấu giá ---");
            Category dummyCat = new Category(1, "Antiques", null);
            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "Đồng hồ Rolex Cổ", "Giá khởi điểm 500", dummyCat, null, 500.0, null, 
                LocalDateTime.now().plusHours(1).toString(), AuctionStatus.ACTIVED // Tạo mở luôn
            );
            outSeller.writeObject(createReq); outSeller.flush();
            CreateAuctionResponse createRes = readExpectedResponse(inSeller, CreateAuctionResponse.class);
            
            int auctionId = createRes.getAuction().getId();
            logAction(seller, "Đã tạo và mở phòng ID: " + auctionId);

            // Các Buyer Join phòng (Để bắt đầu lắng nghe, bỏ qua response join ở đây cho gọn)
            outB1.writeObject(new JoinAuctionRoomRequest(auctionId)); outB1.flush();
            readExpectedResponse(inB1, JoinAuctionRoomResponse.class);
            
            outB2.writeObject(new JoinAuctionRoomRequest(auctionId)); outB2.flush();
            readExpectedResponse(inB2, JoinAuctionRoomResponse.class);
            
            outB3.writeObject(new JoinAuctionRoomRequest(auctionId)); outB3.flush();
            readExpectedResponse(inB3, JoinAuctionRoomResponse.class);

            // =================================================================
            // BƯỚC 3: TEST CÁC LUỒNG ĐẶT GIÁ (CORE LOGIC)
            // =================================================================
            logSystem("--- BƯỚC 3: Giao tranh đặt giá ---");

            // TC_PB01: Buyer 1 đặt giá hợp lệ (1000 > 500)
            logAction(buyer1, "TC_PB01: Đặt giá 1000.0 VND (Hợp lệ)...");
            outB1.writeObject(new PlaceBidRequest(auctionId, 1000.0)); outB1.flush();
            PlaceBidResponse resB1 = readExpectedResponse(inB1, PlaceBidResponse.class);
            describePlaceBidResponse(resB1, "TC_PB01");
            if (resB1.isSuccess()) logSystem("✅ ĐÚNG: Hệ thống chấp nhận giá hợp lệ.");

            // TC_PB02: Buyer 2 đặt giá THẤP HƠN giá hiện tại (800 < 1000)
            logAction(buyer2, "TC_PB02: Đặt giá 800.0 VND (Thấp hơn người trước)...");
            outB2.writeObject(new PlaceBidRequest(auctionId, 800.0)); outB2.flush();
            PlaceBidResponse resB2 = readExpectedResponse(inB2, PlaceBidResponse.class);
            describePlaceBidResponse(resB2, "TC_PB02");
            if (!resB2.isSuccess()) logSystem("✅ ĐÚNG: Hệ thống đã chặn giá thấp hơn.");

            // TC_PB03: Buyer 3 đặt giá cao (2000) nhưng KHÔNG ĐỦ TIỀN (Số dư = 0)
            logAction(buyer3, "TC_PB03: Đặt giá 2000.0 VND nhưng ví rỗng (0 VND)...");
            outB3.writeObject(new PlaceBidRequest(auctionId, 2000.0)); outB3.flush();
            PlaceBidResponse resB3 = readExpectedResponse(inB3, PlaceBidResponse.class);
            describePlaceBidResponse(resB3, "TC_PB03");
            if (!resB3.isSuccess()) logSystem("✅ ĐÚNG: Hệ thống đã chặn vì không đủ số dư ví.");

            // =================================================================
            // BƯỚC 4: TEST ĐẶT GIÁ SAU KHI PHÒNG ĐÓNG
            // =================================================================
            logSystem("--- BƯỚC 4: Đóng phòng và test chốt sổ ---");
            logAction(seller, "Ép hủy/đóng phòng đấu giá...");
            outSeller.writeObject(new ChangeAuctionStatusRequest(auctionId, AuctionStatus.CANCELLED));
            outSeller.flush();
            readExpectedResponse(inSeller, ChangeAuctionStatusResponse.class);

            // TC_PB04: Buyer 1 thử đặt thêm giá khi phòng đã đóng
            logAction(buyer1, "TC_PB04: Cố chấp đặt giá 5000.0 VND khi phòng đã ĐÓNG...");
            outB1.writeObject(new PlaceBidRequest(auctionId, 5000.0)); outB1.flush();
            PlaceBidResponse resLateBid = readExpectedResponse(inB1, PlaceBidResponse.class);
            describePlaceBidResponse(resLateBid, "TC_PB04");
            if (!resLateBid.isSuccess()) logSystem("✅ ĐÚNG: Hệ thống đã chặn đặt giá ở phòng đã đóng.");

        } catch (Exception e) {
            logError("BIDDING_SCENARIO", "Xảy ra lỗi nghiêm trọng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH
    // =========================================================================

    /**
     * Hàm Đọc Response Thông Minh: 
     * Bỏ qua các Event bất đồng bộ (như NewBidEvent) bắn về để tìm đúng Response mong đợi.
     */
    @SuppressWarnings("unchecked")
    private static <T> T readExpectedResponse(ObjectInputStream in, Class<T> expectedClass) throws Exception {
        while (true) {
            Object obj = in.readObject();
            if (expectedClass.isInstance(obj)) {
                return expectedClass.cast(obj);
            } else {
                // Nuốt các event bất đồng bộ rác
                logSystem("   [Bỏ qua gói tin bất đồng bộ: " + obj.getClass().getSimpleName() + "]");
            }
        }
    }

    private static void setupSession(ObjectOutputStream out, ObjectInputStream in, String user, String pass) throws Exception {
        out.writeObject(new SignupRequest(user, user + "@test.com", pass, pass)); out.flush();
        readExpectedResponse(in, SignupResponse.class);
        out.writeObject(new LoginRequest(user, pass)); out.flush();
        readExpectedResponse(in, LoginResponse.class);
    }

    private static void describePlaceBidResponse(PlaceBidResponse res, String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> PLACE_BID] (%s)\n", tag));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ CHẤP NHẬN" : "❌ TỪ CHỐI").append("\n");
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