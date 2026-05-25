package com.groupproject.client.test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.request.CreateAuctionRequest;
import com.groupproject.shared.network.request.GetMyAuctionsRequest;
import com.groupproject.shared.network.request.LoginRequest;
import com.groupproject.shared.network.request.SignupRequest;
import com.groupproject.shared.network.response.CreateAuctionResponse;
import com.groupproject.shared.network.response.GetMyAuctionsResponse;

/**
 * Test Suite 02: Auction CRUD & Data Isolation (AuctionCRUDTest)
 * Tập trung: Validate tạo phòng, luồng tạo phòng thành công, và cách ly dữ liệu giữa các Seller.
 */
public class AuctionCRUDTest {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final Object consoleLock = new Object();

    public static void main(String[] args) {
        logSystem("=== KHỞI CHẠY KIỂM THỬ TẠO PHÒNG & CÁCH LY DỮ LIỆU (CRUD_TEST) ===");

        // Chạy tuần tự các kịch bản
        testCreateAuctionValidation();
        testCreateAuctionSuccess();
        testSellerDataIsolation();

        logSystem("=== HỆ THỐNG KIỂM THỬ CRUD_TEST HOÀN THÀNH ===");
    }

    // --- CASE 01: Validate Tạo phòng thất bại (Thời gian sai) ---
    private static void testCreateAuctionValidation() {
        logAction("VALIDATION", "TC_CA01: Kiểm tra Validate - Bố trí EndTime nằm trong QUÁ KHỨ...");
        String dynUser = "seller_val_" + System.currentTimeMillis();

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // 1. Đăng ký & Đăng nhập
            setupSession(out, in, dynUser, "pass123");

            // 2. Tạo phòng với EndTime đã qua
            Category dummyCat = new Category(1, "Electronics", null);
            String pastTime = LocalDateTime.now().minusDays(1).toString(); // Hôm qua
            
            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "iPhone Lỗi Thời", "Sản phẩm test", dummyCat, null, 1000.0, null, 
                pastTime, AuctionStatus.WAITING
            );
            
            out.writeObject(createReq);
            out.flush();

            Object res = in.readObject();
            if (res instanceof CreateAuctionResponse) {
                CreateAuctionResponse createRes = (CreateAuctionResponse) res;
                describeCreateResponse(createRes, "TC_CA01");
                if (!createRes.isSuccess()) {
                    logSystem("✅ HỆ THỐNG XỬ LÝ ĐÚNG: Đã chặn việc tạo phòng có thời gian không hợp lệ.");
                }
            }
        } catch (Exception e) {
            logError("TC_CA01", "Lỗi: " + e.getMessage());
        }
    }

    // --- CASE 02: Tạo phòng thành công (WAITING) ---
    private static void testCreateAuctionSuccess() {
        logAction("CRUD", "TC_CA02: Seller tạo một phòng đấu giá hợp lệ ở trạng thái WAITING...");
        String dynUser = "seller_crud_" + System.currentTimeMillis();

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // 1. Đăng ký & Đăng nhập
            setupSession(out, in, dynUser, "pass123");

            // 2. Tạo phòng hợp lệ
            Category dummyCat = new Category(1, "Art", null);
            String futureTime = LocalDateTime.now().plusHours(2).toString();
            
            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "Tranh Picasso Fake", "Bản copy đẹp", dummyCat, null, 5000.0, null, 
                futureTime, AuctionStatus.WAITING
            );
            
            out.writeObject(createReq);
            out.flush();

            Object res = in.readObject();
            if (res instanceof CreateAuctionResponse) {
                describeCreateResponse((CreateAuctionResponse) res, "TC_CA02");
            }
        } catch (Exception e) {
            logError("TC_CA02", "Lỗi: " + e.getMessage());
        }
    }

    // --- CASE 03: Cách ly dữ liệu (Data Isolation) ---
    private static void testSellerDataIsolation() {
        logAction("ISOLATION", "TC_GMA03: Kiểm tra cách ly dữ liệu - Seller B không thấy phòng của Seller A...");
        
        long timestamp = System.currentTimeMillis();
        String sellerA = "sellerA_" + timestamp;
        String sellerB = "sellerB_" + timestamp;

        // BƯỚC 1: Seller A tạo một phòng mới
        logAction("ISOLATION", "-> Seller A đang đăng nhập và tạo phòng...");
        try (Socket socketA = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream outA = new ObjectOutputStream(socketA.getOutputStream());
             ObjectInputStream inA = new ObjectInputStream(socketA.getInputStream())) {

            setupSession(outA, inA, sellerA, "pass123");

            Category dummyCat = new Category(1, "Vehicles", null);
            CreateAuctionRequest createReq = new CreateAuctionRequest(
                "Xe Máy Của Seller A", "Xe xịn", dummyCat, null, 15000.0, null, 
                LocalDateTime.now().plusHours(1).toString(), AuctionStatus.WAITING
            );
            outA.writeObject(createReq);
            outA.flush();
            inA.readObject(); // Bỏ qua response tạo phòng
        } catch (Exception e) {
            logError("TC_GMA03_A", "Lỗi Seller A: " + e.getMessage());
        }

        // BƯỚC 2: Seller B lấy danh sách My Auctions
        logAction("ISOLATION", "-> Seller B đăng nhập và lấy danh sách My Auctions...");
        try (Socket socketB = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream outB = new ObjectOutputStream(socketB.getOutputStream());
             ObjectInputStream inB = new ObjectInputStream(socketB.getInputStream())) {

            setupSession(outB, inB, sellerB, "pass123");

            outB.writeObject(new GetMyAuctionsRequest());
            outB.flush();

            Object res = inB.readObject();
            if (res instanceof GetMyAuctionsResponse) {
                GetMyAuctionsResponse myRes = (GetMyAuctionsResponse) res;
                describeGetMyAuctionsResponse(myRes, "TC_GMA03_B");
                
                if (myRes.isSuccess() && myRes.getMyAuctions().isEmpty()) {
                    logSystem("✅ HỆ THỐNG XỬ LÝ ĐÚNG: Danh sách của Seller B trống (Không bị lộ phòng của Seller A).");
                } else {
                    logError("ISOLATION", "❌ LỖI BẢO MẬT: Seller B nhìn thấy phòng của người khác!");
                }
            }
        } catch (Exception e) {
            logError("TC_GMA03_B", "Lỗi Seller B: " + e.getMessage());
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH (UTILITIES)
    // =========================================================================

    /**
     * Hàm helper thực hiện nhanh việc Đăng ký rồi Đăng nhập trên cùng 1 Socket 
     * để thiết lập Session Context cho các request phía sau.
     */
    private static void setupSession(ObjectOutputStream out, ObjectInputStream in, String username, String password) throws Exception {
        // Đăng ký
        out.writeObject(new SignupRequest(username, username + "@test.com", password, password));
        out.flush();
        in.readObject(); 

        // Đăng nhập để lưu session trên Server (ClientContext)
        out.writeObject(new LoginRequest(username, password));
        out.flush();
        in.readObject(); 
    }

    private static void describeCreateResponse(CreateAuctionResponse res, String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> CREATE_AUCTION] (%s)\n", tag));
        sb.append("   Trạng thái: ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        sb.append("   Tin nhắn  : ").append(res.getMessage()).append("\n");
        if (res.isSuccess() && res.getAuction() != null) {
            sb.append("   ID Phòng  : ").append(res.getAuction().getId()).append("\n");
        }
        sb.append("--------------------------------------------------\n");
        printLog(sb.toString());
    }

    private static void describeGetMyAuctionsResponse(GetMyAuctionsResponse res, String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📩 [SERVER -> MY_AUCTIONS] (%s)\n", tag));
        sb.append("   Kết quả   : ").append(res.isSuccess() ? "✅ THÀNH CÔNG" : "❌ THẤT BẠI").append("\n");
        if (res.getMyAuctions() != null) {
            sb.append("   Số lượng  : ").append(res.getMyAuctions().size()).append(" phòng thuộc sở hữu.\n");
        }
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