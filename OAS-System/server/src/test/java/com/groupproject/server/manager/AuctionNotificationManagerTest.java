package com.groupproject.server.manager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.groupproject.server.pattern.observer.AuctionObserver;

class AuctionNotificationManagerTest {

    private AuctionNotificationManager manager;
    private AuctionObserver mockObserver1;
    private AuctionObserver mockObserver2;
 
    @BeforeEach
    void setUp() {
        manager       = AuctionNotificationManager.getInstance();
        mockObserver1 = mock(AuctionObserver.class);
        mockObserver2 = mock(AuctionObserver.class);
    }
 
    // -------------------------------------------------------------------------
    // SINGLETON
    // -------------------------------------------------------------------------
 
    @Test
    @DisplayName("[Singleton] Hai lần gọi getInstance() phải trả về cùng một object")
    void testSingleton() {
        AuctionNotificationManager a = AuctionNotificationManager.getInstance();
        AuctionNotificationManager b = AuctionNotificationManager.getInstance();
        assertNotNull(a);
        // assertSame so sánh địa chỉ bộ nhớ (==), không phải .equals()
        assertSame(a, b, "Phải là cùng một object trong bộ nhớ");
    }
 
    // -------------------------------------------------------------------------
    // VALIDATION — gộp null của cả addObserver lẫn removeObserver vào 1 test
    // -------------------------------------------------------------------------
 
    @Test
    @DisplayName("[Validation] Tham số null luôn ném IllegalArgumentException")
    void testNullParams_ThrowException() {
        // addObserver: auctionId null
        assertThrows(IllegalArgumentException.class,
                () -> manager.addObserver(null, mockObserver1));
 
        // addObserver: observer null
        assertThrows(IllegalArgumentException.class,
                () -> manager.addObserver("ROOM_A", null));
 
        // removeObserver: auctionId null
        assertThrows(IllegalArgumentException.class,
                () -> manager.removeObserver(null, mockObserver1));
 
        // removeObserver: observer null
        assertThrows(IllegalArgumentException.class,
                () -> manager.removeObserver("ROOM_A", null));
    }
 
    // -------------------------------------------------------------------------
    // LOGIC CƠ BẢN
    // -------------------------------------------------------------------------
 
    @Test
    @DisplayName("[Logic] Add → Notify → observer nhận được; Remove → không nhận nữa")
    void testAddAndRemove() {
        String roomId = "ROOM_BASIC";
 
        // --- Add ---
        manager.addObserver(roomId, mockObserver1);
        manager.notifyAuctionStarted(roomId);
        // times(1): xác nhận được gọi đúng 1 lần với đúng tham số
        verify(mockObserver1, times(1)).onAuctionStarted(roomId);
 
        // --- Remove ---
        manager.removeObserver(roomId, mockObserver1);
        manager.notifyAuctionEnded(roomId);
        // never(): xác nhận KHÔNG được gọi sau khi đã remove
        verify(mockObserver1, never()).onAuctionEnded(roomId);
    }
 
    @Test
    @DisplayName("[Logic] Notify chỉ gửi đúng room, không lan sang room khác")
    void testNotifyIsolation() {
        manager.addObserver("ROOM_X", mockObserver1);
        manager.addObserver("ROOM_Y", mockObserver2);
 
        manager.notifyAuctionStarted("ROOM_X"); // chỉ notify ROOM_X
 
        verify(mockObserver1, times(1)).onAuctionStarted("ROOM_X");
        verify(mockObserver2, never()).onAuctionStarted(anyString()); // ROOM_Y không nhận gì
 
        // Cleanup
        manager.removeObserver("ROOM_X", mockObserver1);
        manager.removeObserver("ROOM_Y", mockObserver2);
    }
 
    @Test
    @DisplayName("[Logic] Remove observer không tồn tại hoặc room không tồn tại → không crash")
    void testRemoveGhost_NoException() {
        // observer chưa từng được add
        assertDoesNotThrow(() -> manager.removeObserver("ROOM_GHOST", mockObserver1));
 
        // room chưa từng tồn tại
        assertDoesNotThrow(() -> manager.removeObserver("ROOM_PHANTOM_XYZ", mockObserver1));
    }
 
    @Test
    @DisplayName("[Logic] Xóa observer cuối → room bị auto-cleanup, observer mới vào room sạch")
    void testAutoCleanup_LastObserver() {
        String roomId = "ROOM_CLEANUP";
        manager.addObserver(roomId, mockObserver1);
 
        // Xóa người cuối → room phải biến mất khỏi map
        manager.removeObserver(roomId, mockObserver1);
 
        // Thêm observer2 vào cùng roomId → phải là room hoàn toàn mới
        manager.addObserver(roomId, mockObserver2);
        manager.notifyAuctionStarted(roomId);
 
        verify(mockObserver1, never()).onAuctionStarted(roomId); // observer1 đã bị xóa
        verify(mockObserver2, times(1)).onAuctionStarted(roomId); // observer2 nhận đúng 1 lần
 
        // Cleanup
        manager.removeObserver(roomId, mockObserver2);
    }
 
    @Test
    @DisplayName("[Concurrency] Remove observer cuối + add mới đồng thời → không mất observer")
    void testConcurrency_RemoveLastAndAddNew_NoObserverLost() throws InterruptedException {
        String roomId  = "ROOM_RACE";
        int trialCount = 100;
        int failCount  = 0;

        for (int i = 0; i < trialCount; i++) {
            AuctionObserver lastObs = mock(AuctionObserver.class);
            AuctionObserver newObs  = mock(AuctionObserver.class);
            manager.addObserver(roomId, lastObs);

            // Thứ tự đúng cần kiểm tra:
            // 1. remove lastObs (room về rỗng → bị xóa khỏi map)
            // 2. add newObs     (tạo lại room mới)
            // Nếu compute() atomic, bước 2 sẽ không bị xóa nhầm bởi bước 1
            CountDownLatch removeDone = new CountDownLatch(1);
            CountDownLatch addDone    = new CountDownLatch(1);

        new Thread(() -> {
            try { manager.removeObserver(roomId, lastObs); }
            finally { removeDone.countDown(); }
        }).start();

        // Đảm bảo add chỉ chạy SAU KHI remove hoàn thành
        // Đây là điều kiện đủ để kiểm tra race condition thật sự:
        // compute() trong remove có trả về null (xóa room) rồi,
        // liệu addObserver tiếp theo có tạo lại room đúng không?
        new Thread(() -> {
            try {
                removeDone.await(); // chờ remove xong hẳn
                manager.addObserver(roomId, newObs);
            } catch (InterruptedException ignored) {}
            finally { addDone.countDown(); }
        }).start();

        addDone.await(2, TimeUnit.SECONDS);

        AtomicInteger received = new AtomicInteger(0);
        doAnswer(inv -> { received.incrementAndGet(); return null; })
                .when(newObs).onAuctionStarted(roomId);

        manager.notifyAuctionStarted(roomId);

        if (received.get() == 0) failCount++;

        // Cleanup
        manager.removeObserver(roomId, newObs);
    }

    assertEquals(0, failCount,
            failCount + "/" + trialCount + " lần observer bị mất!");
    }

    /**
     * Test khi có người ra giá mới, TẤT CẢ mọi người trong phòng đều phải nhận được thông báo.
     */
    @Test
    @DisplayName("Test notifyBidUpdated")
    void testNotifyBidUpdated() {
        String auctionId = "ROOM_BID";
        // Cho cả 2 người vào cùng 1 phòng
        manager.addObserver(auctionId, mockObserver1);
        manager.addObserver(auctionId, mockObserver2);

        // Báo có giá mới: Giá 500.0, người ra giá "USER_A"
        manager.notifyBidUpdated(auctionId, 500.0, "USER_A");

        // Xác nhận (verify) cả mock 1 và mock 2 đều được Manager gọi đúng hàm với đúng tham số
        verify(mockObserver1).onBidUpdated(auctionId, 500.0, "USER_A");
        verify(mockObserver2).onBidUpdated(auctionId, 500.0, "USER_A");
    }

    /**
     * Test phát thông báo đấu giá bắt đầu.
     */
    @Test
    @DisplayName("Test notifyAuctionStarted")
    void testNotifyAuctionStarted() {
        String auctionId = "ROOM_START";
        manager.addObserver(auctionId, mockObserver1);

        manager.notifyAuctionStarted(auctionId);

        // Kiểm tra mockObserver1 nhận được lệnh
        verify(mockObserver1).onAuctionStarted(auctionId);
    }

    /**
     * Test phát thông báo hết giờ đặt giá.
     */
    @Test
    @DisplayName("Test notifyAuctionEnded")
    void testNotifyAuctionEnded() {
        String auctionId = "ROOM_END";
        manager.addObserver(auctionId, mockObserver1);

        manager.notifyAuctionEnded(auctionId);

        verify(mockObserver1).onAuctionEnded(auctionId);
    }

    /**
     * Test cực kỳ quan trọng: Khi chốt giao dịch, phải thông báo và sau đó XÓA LUÔN PHÒNG KHỎI RAM.
     */
    @Test
    @DisplayName("Test notifyAuctionFinished - Xóa phòng sau khi kết thúc")
    void testNotifyAuctionFinished() {
        String auctionId = "ROOM_FINISH";
        manager.addObserver(auctionId, mockObserver1);

        // Phát thông báo chốt giá
        manager.notifyAuctionFinished(auctionId, "WINNER_01", 1500.0);

        // 1. Kiểm tra: mockObserver1 có nhận được thông báo chốt giá thành công không?
        verify(mockObserver1).onAuctionFinished(auctionId, "WINNER_01", 1500.0);

        // 2. Kiểm tra nghiệp vụ ẩn: Dòng `roomObservers.remove(auctionId)` trong code của bạn có chạy không?
        // Cách test: Ta giả vờ phát thêm 1 thông báo Started vào cái phòng vừa Finish đó.
        manager.notifyAuctionStarted(auctionId);
        
        // Nếu phòng đã thực sự bị xóa khỏi Map, thì mockObserver1 sẽ KHÔNG nhận được thông báo Started này (never).
        verify(mockObserver1, never()).onAuctionStarted(auctionId);
    }

    /**
     * Tương tự Finished, khi Hủy phòng cũng phải thông báo xong rồi dọn dẹp RAM.
     */
    @Test
    @DisplayName("Test notifyAuctionCancelled - Xóa phòng sau khi hủy")
    void testNotifyAuctionCancelled() {
        String auctionId = "ROOM_CANCEL";
        manager.addObserver(auctionId, mockObserver1);

        manager.notifyAuctionCancelled(auctionId, "Người bán rút lại sản phẩm");

        // 1. Kiểm tra gửi thông báo hủy thành công
        verify(mockObserver1).onAuctionCancelled(auctionId, "Người bán rút lại sản phẩm");

        // 2. Kiểm tra xem phòng đã bị xóa khỏi ConcurrentHashMap chưa
        manager.notifyAuctionEnded(auctionId);
        verify(mockObserver1, never()).onAuctionEnded(auctionId);
    }
    
    /**
     * Test rủi ro (Edge case): Gọi hàm notify tới một cái phòng trống trơn, hoặc phòng không tồn tại.
     * Code xịn là code không bị văng lỗi (NullPointerException) trong trường hợp này.
     */
    @Test
    @DisplayName("Không lỗi khi gửi thông báo tới phòng không tồn tại")
    void testNotifyNonExistentRoom() {
        // assertDoesNotThrow đảm bảo rằng đoạn code bên trong chạy trơn tru, không bắn ra cái Exception nào cả
        assertDoesNotThrow(() -> {
            manager.notifyAuctionStarted("GHOST_ROOM");
        });
    }
}