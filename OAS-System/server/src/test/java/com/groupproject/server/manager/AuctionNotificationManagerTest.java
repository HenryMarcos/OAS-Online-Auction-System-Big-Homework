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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    @DisplayName("[Logic] notifyBidUpdated: tất cả observer trong room đều nhận đúng thông báo")
    void testNotifyBidUpdated_AllObserversReceive() {
        // Mục đích: kiểm tra core behavior của Observer Pattern —
        // khi có giá mới, TẤT CẢ observer trong room phải được thông báo,
        // không bỏ sót ai, không gọi sai tham số.
        String roomId = "ROOM_BID_MULTI";
        manager.addObserver(roomId, mockObserver1);
        manager.addObserver(roomId, mockObserver2);

        manager.notifyBidUpdated(roomId, 500.0, "USER_A");

        // Xác nhận cả hai nhận đúng method, đúng 3 tham số, đúng 1 lần
        verify(mockObserver1, times(1)).onBidUpdated(roomId, 500.0, "USER_A");
        verify(mockObserver2, times(1)).onBidUpdated(roomId, 500.0, "USER_A");

        // Cleanup
        manager.removeObserver(roomId, mockObserver1);
        manager.removeObserver(roomId, mockObserver2);
    }

    @Test
    @DisplayName("[Logic] notifyBidUpdated: room không tồn tại → không crash")
    void testNotifyBidUpdated_EmptyRoom_NoException() {
        // Mục đích: đảm bảo defensive check hoạt động đúng —
        // gọi notify vào room chưa có ai (getRoomObservers trả về null)
        // không được ném NullPointerException hay bất kỳ exception nào.
        assertDoesNotThrow(() -> manager.notifyBidUpdated("ROOM_BID_GHOST", 500.0, "USER_A"));
    }

    @Test
    @DisplayName("[Logic] notifyBidUpdated: chỉ notify đúng room, không lan sang room khác")
    void testNotifyBidUpdated_Isolation() {
        // Mục đích: kiểm tra thông báo không bị "rò rỉ" sang room khác —
        // observer ở ROOM_B không được nhận thông báo của ROOM_A,
        // dù cả hai đang tồn tại cùng lúc trong map.
        String roomA = "ROOM_BID_A";
        String roomB = "ROOM_BID_B";
        manager.addObserver(roomA, mockObserver1);
        manager.addObserver(roomB, mockObserver2);

        manager.notifyBidUpdated(roomA, 500.0, "USER_A"); // chỉ notify roomA

        verify(mockObserver1, times(1)).onBidUpdated(roomA, 500.0, "USER_A");
        verify(mockObserver2, never()).onBidUpdated(anyString(), anyDouble(), anyString());

        // Cleanup
        manager.removeObserver(roomA, mockObserver1);
        manager.removeObserver(roomB, mockObserver2);
    }

    @Test
    @DisplayName("[Logic] notifyBidUpdated: observer nhận đúng 1 lần, đúng tham số")
    void testNotifyBidUpdated_SingleObserver_CorrectParams() {
        // Mục đích: kiểm tra tính chính xác của tham số được truyền xuống observer —
        // đúng auctionId, đúng newBidAmount, đúng highestBidderId,
        // và chỉ được gọi đúng 1 lần (không bị gọi thừa).
        String roomId = "ROOM_BID_SINGLE";
        manager.addObserver(roomId, mockObserver1);

        manager.notifyBidUpdated(roomId, 750.0, "USER_B");

        // Kiểm tra kỹ cả 3 tham số: auctionId, newBidAmount, highestBidderId
        verify(mockObserver1, times(1)).onBidUpdated(roomId, 750.0, "USER_B");
        // Đảm bảo không bị gọi với tham số sai
        verify(mockObserver1, never()).onBidUpdated(anyString(), eq(500.0), anyString());

        // Cleanup
        manager.removeObserver(roomId, mockObserver1);
    }   

    // =========================================================================
    // notifyAuctionStarted
    // =========================================================================

    @Test
    @DisplayName("[Logic] notifyAuctionStarted: observer nhận đúng thông báo")
    void testNotifyAuctionStarted_ObserverReceives() {
        // Mục đích: happy path cơ bản — add observer vào room rồi notify,
        // observer phải nhận đúng method với đúng auctionId, đúng 1 lần.
        manager.addObserver("ROOM_START", mockObserver1);
        manager.notifyAuctionStarted("ROOM_START");
        verify(mockObserver1, times(1)).onAuctionStarted("ROOM_START");
        manager.removeObserver("ROOM_START", mockObserver1);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionStarted: nhiều observer đều nhận")
    void testNotifyAuctionStarted_MultipleObservers() {
        // Mục đích: kiểm tra core của Observer Pattern —
        // Manager phải iterate qua toàn bộ danh sách,
        // không được bỏ sót bất kỳ observer nào trong room.
        manager.addObserver("ROOM_START_MULTI", mockObserver1);
        manager.addObserver("ROOM_START_MULTI", mockObserver2);
        manager.notifyAuctionStarted("ROOM_START_MULTI");
        verify(mockObserver1, times(1)).onAuctionStarted("ROOM_START_MULTI");
        verify(mockObserver2, times(1)).onAuctionStarted("ROOM_START_MULTI");
        manager.removeObserver("ROOM_START_MULTI", mockObserver1);
        manager.removeObserver("ROOM_START_MULTI", mockObserver2);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionStarted: room không tồn tại → không crash")
    void testNotifyAuctionStarted_EmptyRoom_NoException() {
        // Mục đích: kiểm tra defensive check "if (room != null && !room.isEmpty())" —
        // getRoomObservers() trả về null không được gây NullPointerException.
        assertDoesNotThrow(() -> manager.notifyAuctionStarted("ROOM_START_GHOST"));
    }

    @Test
    @DisplayName("[Logic] notifyAuctionStarted: không lan sang room khác")
    void testNotifyAuctionStarted_Isolation() {
        // Mục đích: đảm bảo Manager notify đúng room —
        // observer ở ROOM_B không được nhận thông báo của ROOM_A
        // dù cả hai đang tồn tại cùng lúc trong map.
        manager.addObserver("ROOM_START_A", mockObserver1);
        manager.addObserver("ROOM_START_B", mockObserver2);

        manager.notifyAuctionStarted("ROOM_START_A"); // chỉ notify ROOM_A

        verify(mockObserver1, times(1)).onAuctionStarted("ROOM_START_A");
        // anyString(): dù Manager gọi với auctionId nào, mockObserver2 cũng không được nhận
        verify(mockObserver2, never()).onAuctionStarted(anyString());

        manager.removeObserver("ROOM_START_A", mockObserver1);
        manager.removeObserver("ROOM_START_B", mockObserver2);
    }

    // =========================================================================
    // notifyAuctionEnded — đối xứng hoàn toàn với notifyAuctionStarted
    // =========================================================================

    @Test
    @DisplayName("[Logic] notifyAuctionEnded: observer nhận đúng thông báo")
    void testNotifyAuctionEnded_ObserverReceives() {
        // Mục đích: happy path — giống notifyAuctionStarted nhưng kiểm tra
        // đúng method onAuctionEnded được gọi, không nhầm sang onAuctionStarted.
        manager.addObserver("ROOM_END", mockObserver1);
        manager.notifyAuctionEnded("ROOM_END");
        verify(mockObserver1, times(1)).onAuctionEnded("ROOM_END");
        manager.removeObserver("ROOM_END", mockObserver1);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionEnded: nhiều observer đều nhận")
    void testNotifyAuctionEnded_MultipleObservers() {
        // Mục đích: giống notifyAuctionStarted — tất cả observer trong room
        // phải nhận thông báo kết thúc, không bỏ sót ai.
        manager.addObserver("ROOM_END_MULTI", mockObserver1);
        manager.addObserver("ROOM_END_MULTI", mockObserver2);
        manager.notifyAuctionEnded("ROOM_END_MULTI");
        verify(mockObserver1, times(1)).onAuctionEnded("ROOM_END_MULTI");
        verify(mockObserver2, times(1)).onAuctionEnded("ROOM_END_MULTI");
        manager.removeObserver("ROOM_END_MULTI", mockObserver1);
        manager.removeObserver("ROOM_END_MULTI", mockObserver2);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionEnded: room không tồn tại → không crash")
    void testNotifyAuctionEnded_EmptyRoom_NoException() {
        // Mục đích: giống notifyAuctionStarted — defensive check
        // ngăn NPE khi room là null hoặc không tồn tại.
        assertDoesNotThrow(() -> manager.notifyAuctionEnded("ROOM_END_GHOST"));
    }

    @Test
    @DisplayName("[Logic] notifyAuctionEnded: không lan sang room khác")
    void testNotifyAuctionEnded_Isolation() {
        // Mục đích: giống notifyAuctionStarted — thông báo kết thúc
        // của ROOM_A không được "rò rỉ" sang observer đang ở ROOM_B.
        manager.addObserver("ROOM_END_A", mockObserver1);
        manager.addObserver("ROOM_END_B", mockObserver2);

        manager.notifyAuctionEnded("ROOM_END_A"); // chỉ notify ROOM_A

        verify(mockObserver1, times(1)).onAuctionEnded("ROOM_END_A");
        verify(mockObserver2, never()).onAuctionEnded(anyString());

        manager.removeObserver("ROOM_END_A", mockObserver1);
        manager.removeObserver("ROOM_END_B", mockObserver2);
    }
    
    // =========================================================================
    // notifyAuctionFinished
    // =========================================================================

    @Test
    @DisplayName("[Logic] notifyAuctionFinished: observer nhận đúng thông báo")
    void testNotifyAuctionFinished_ObserverReceives() {
        // Mục đích: happy path — verify đúng method, đúng 3 tham số,
        // đúng 1 lần. Đặc biệt kiểm tra winnerId và finalPrice
        // được truyền xuống observer không bị sai lệch.
        manager.addObserver("ROOM_FIN", mockObserver1);

        manager.notifyAuctionFinished("ROOM_FIN", "USER_WIN", 1_000_000.0);

        verify(mockObserver1, times(1)).onAuctionFinished("ROOM_FIN", "USER_WIN", 1_000_000.0);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionFinished: nhiều observer đều nhận")
    void testNotifyAuctionFinished_MultipleObservers() {
        // Mục đích: tất cả observer trong room đều nhận thông báo
        // kết thúc phiên trước khi room bị xóa.
        manager.addObserver("ROOM_FIN_MULTI", mockObserver1);
        manager.addObserver("ROOM_FIN_MULTI", mockObserver2);

        manager.notifyAuctionFinished("ROOM_FIN_MULTI", "USER_WIN", 1_000_000.0);

        verify(mockObserver1, times(1)).onAuctionFinished("ROOM_FIN_MULTI", "USER_WIN", 1_000_000.0);
        verify(mockObserver2, times(1)).onAuctionFinished("ROOM_FIN_MULTI", "USER_WIN", 1_000_000.0);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionFinished: room không tồn tại → không crash")
    void testNotifyAuctionFinished_EmptyRoom_NoException() {
        // Mục đích: defensive check — room null không gây NPE.
        assertDoesNotThrow(() -> manager.notifyAuctionFinished("ROOM_FIN_GHOST", "USER_WIN", 1_000_000.0));
    }

    @Test
    @DisplayName("[Logic] notifyAuctionFinished: không lan sang room khác")
    void testNotifyAuctionFinished_Isolation() {
        // Mục đích: observer ở ROOM_B không nhận thông báo của ROOM_A.
        manager.addObserver("ROOM_FIN_A", mockObserver1);
        manager.addObserver("ROOM_FIN_B", mockObserver2);

        manager.notifyAuctionFinished("ROOM_FIN_A", "USER_WIN", 1_000_000.0);

        verify(mockObserver1, times(1)).onAuctionFinished("ROOM_FIN_A", "USER_WIN", 1_000_000.0);
        verify(mockObserver2, never()).onAuctionFinished(anyString(), anyString(), anyDouble());

        // Cleanup chỉ cần cho ROOM_B vì ROOM_A đã bị tự xóa sau notify
        manager.removeObserver("ROOM_FIN_B", mockObserver2);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionFinished: room bị tự xóa sau khi notify")
    void testNotifyAuctionFinished_RoomAutoRemoved() {
        // Mục đích: sau khi notify xong, roomObservers.remove() phải được gọi —
        // observer mới add vào cùng roomId sau đó không được nhận thông báo cũ,
        // chứng tỏ room đã bị xóa và tạo lại từ đầu.
        manager.addObserver("ROOM_FIN_CLEAN", mockObserver1);

        manager.notifyAuctionFinished("ROOM_FIN_CLEAN", "USER_WIN", 1_000_000.0);

        // Thêm observer2 vào cùng roomId sau khi room đã bị xóa
        manager.addObserver("ROOM_FIN_CLEAN", mockObserver2);
        manager.notifyAuctionStarted("ROOM_FIN_CLEAN");

        // observer1 đã bị xóa cùng room, không nhận được gì thêm
        verify(mockObserver1, never()).onAuctionStarted("ROOM_FIN_CLEAN");
        // observer2 vào room mới, nhận đúng 1 lần
        verify(mockObserver2, times(1)).onAuctionStarted("ROOM_FIN_CLEAN");

        // Cleanup
        manager.removeObserver("ROOM_FIN_CLEAN", mockObserver2);
    }

    // =========================================================================
    // notifyAuctionCancelled — đối xứng với notifyAuctionFinished,
    // chỉ khác tham số reason thay vì winnerId + finalPrice
    // =========================================================================

    @Test
    @DisplayName("[Logic] notifyAuctionCancelled: observer nhận đúng thông báo")
    void testNotifyAuctionCancelled_ObserverReceives() {
        // Mục đích: happy path — verify đúng method, đúng auctionId và reason,
        // đúng 1 lần.
        manager.addObserver("ROOM_CANCEL", mockObserver1);

        manager.notifyAuctionCancelled("ROOM_CANCEL", "Không đủ người tham gia");

        verify(mockObserver1, times(1)).onAuctionCancelled("ROOM_CANCEL", "Không đủ người tham gia");
    }

    @Test
    @DisplayName("[Logic] notifyAuctionCancelled: nhiều observer đều nhận")
    void testNotifyAuctionCancelled_MultipleObservers() {
        // Mục đích: tất cả observer đều nhận lý do hủy
        // trước khi room bị xóa.
        manager.addObserver("ROOM_CANCEL_MULTI", mockObserver1);
        manager.addObserver("ROOM_CANCEL_MULTI", mockObserver2);

        manager.notifyAuctionCancelled("ROOM_CANCEL_MULTI", "Không đủ người tham gia");

        verify(mockObserver1, times(1)).onAuctionCancelled("ROOM_CANCEL_MULTI", "Không đủ người tham gia");
        verify(mockObserver2, times(1)).onAuctionCancelled("ROOM_CANCEL_MULTI", "Không đủ người tham gia");
    }

    @Test
    @DisplayName("[Logic] notifyAuctionCancelled: room không tồn tại → không crash")
    void testNotifyAuctionCancelled_EmptyRoom_NoException() {
        // Mục đích: defensive check — room null không gây NPE.
        assertDoesNotThrow(() -> manager.notifyAuctionCancelled("ROOM_CANCEL_GHOST", "Lý do bất kỳ"));
    }

    @Test
    @DisplayName("[Logic] notifyAuctionCancelled: không lan sang room khác")
    void testNotifyAuctionCancelled_Isolation() {
        // Mục đích: observer ở ROOM_B không nhận thông báo hủy của ROOM_A.
        manager.addObserver("ROOM_CANCEL_A", mockObserver1);
        manager.addObserver("ROOM_CANCEL_B", mockObserver2);

        manager.notifyAuctionCancelled("ROOM_CANCEL_A", "Không đủ người tham gia");

        verify(mockObserver1, times(1)).onAuctionCancelled("ROOM_CANCEL_A", "Không đủ người tham gia");
        verify(mockObserver2, never()).onAuctionCancelled(anyString(), anyString());

        // Cleanup chỉ cần cho ROOM_B vì ROOM_A đã bị tự xóa sau notify
        manager.removeObserver("ROOM_CANCEL_B", mockObserver2);
    }

    @Test
    @DisplayName("[Logic] notifyAuctionCancelled: room bị tự xóa sau khi notify")
    void testNotifyAuctionCancelled_RoomAutoRemoved() {
        // Mục đích: sau khi notify xong, room phải bị xóa —
        // giống notifyAuctionFinished, đảm bảo không có observer cũ
        // nào còn "ám" trong room sau khi phiên đã bị hủy.
        manager.addObserver("ROOM_CANCEL_CLEAN", mockObserver1);
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