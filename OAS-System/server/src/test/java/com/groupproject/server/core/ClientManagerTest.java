package com.groupproject.server.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.groupproject.shared.network.events.ServerEvent;

public class ClientManagerTest {

    private ClientManager clientManager;
    private ObjectOutputStream mockOut1;
    private ObjectOutputStream mockOut2;
    private ServerEvent mockEvent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        clientManager = ClientManager.INSTANCE;

        // Sử dụng Reflection để dọn sạch dữ liệu cũ của Singleton trước mỗi hàm test
        Field clientsField = ClientManager.class.getDeclaredField("clients");
        clientsField.setAccessible(true);
        ((List<ObjectOutputStream>) clientsField.get(clientManager)).clear();

        Field authenticatedUsersField = ClientManager.class.getDeclaredField("authenticatedUsers");
        authenticatedUsersField.setAccessible(true);
        ((Map<Integer, ObjectOutputStream>) authenticatedUsersField.get(clientManager)).clear();

        Field adminClientsField = ClientManager.class.getDeclaredField("adminClients");
        adminClientsField.setAccessible(true);
        ((Set<ObjectOutputStream>) adminClientsField.get(clientManager)).clear();

        Field auctionRoomsField = ClientManager.class.getDeclaredField("auctionRooms");
        auctionRoomsField.setAccessible(true);
        ((Map<Integer, Set<ObjectOutputStream>>) auctionRoomsField.get(clientManager)).clear();

        // Tạo các đối tượng giả lập (Mock) phục vụ kiểm thử
        mockOut1 = mock(ObjectOutputStream.class);
        mockOut2 = mock(ObjectOutputStream.class);
        mockEvent = mock(ServerEvent.class);
    }

    @Test
    public void testAddAndRemoveGeneralClient() {
        // Kiểm tra thêm client chung vào hệ thống
        clientManager.addClient(mockOut1);
        assertEquals(1, clientManager.getClients().size());
        assertTrue(clientManager.getClients().contains(mockOut1));

        // Kiểm tra xóa client
        clientManager.removeClient(mockOut1);
        assertEquals(0, clientManager.getClients().size());
    }

    @Test
    public void testAddAndRemoveAdminClient() throws IOException {
        // Đăng ký luồng admin
        clientManager.addAdminClient(mockOut1);

        // Phát tín hiệu tới admin và kiểm tra xem admin nhận được không
        clientManager.broadcastToAdmins(mockEvent);
        verify(mockOut1, times(1)).writeObject(mockEvent);

        // Xóa admin ra khỏi luồng dữ liệu
        clientManager.removeClient(mockOut1);
        clientManager.broadcastToAdmins(mockEvent);
        // Số lần nhận vẫn là 1 (không tăng thêm sau khi xóa)
        verify(mockOut1, times(1)).writeObject(mockEvent);
    }

    @Test
    public void testRegisterUserAndDirectMessaging() throws IOException {
        int userId = 99;
        
        // Đăng ký một User đã định danh thành công
        clientManager.registerUser(userId, mockOut1);

        // Gửi tin nhắn trực tiếp (Direct Message) cho cá nhân User này
        clientManager.sendToUser(userId, mockEvent);
        verify(mockOut1, times(1)).writeObject(mockEvent);
        verify(mockOut1, times(1)).flush();
        verify(mockOut1, times(1)).reset();

        // Thử hủy đăng ký và gửi lại, hệ thống không được gửi tiếp
        clientManager.unregisterUser(userId);
        clientManager.sendToUser(userId, mockEvent);
        verify(mockOut1, times(1)).writeObject(mockEvent); // Vẫn chỉ là 1 lần của đợt trước
    }

    @Test
    public void testAuctionRoomSubscriptionAndBroadcast() throws IOException {
        int auctionId = 1001;

        // Cho 2 client đăng ký theo dõi phòng đấu giá mã số 1001
        clientManager.subscribeToAuction(auctionId, mockOut1);
        clientManager.subscribeToAuction(auctionId, mockOut2);

        // Phát thông điệp (ví dụ: có người trả giá mới) vào phòng 1001
        clientManager.broadcastEventToAuction(auctionId, mockEvent);

        // Cả 2 client trong phòng phải nhận được thông điệp này
        verify(mockOut1, times(1)).writeObject(mockEvent);
        verify(mockOut2, times(1)).writeObject(mockEvent);

        // Client 1 rời phòng, phát tiếp thông điệp mới
        clientManager.unsubscribeToAuction(auctionId, mockOut1);
        clientManager.broadcastEventToAuction(auctionId, mockEvent);

        // Client 2 nhận được tổng cộng 2 lần, Client 1 vẫn chỉ dừng lại ở 1 lần
        verify(mockOut1, times(1)).writeObject(mockEvent);
        verify(mockOut2, times(2)).writeObject(mockEvent);
    }

    @Test
    public void testAutoSubscribeAndUnsubscribeByUserId() throws IOException {
        int auctionId = 2002;
        int userId = 77;

        // User 77 đăng nhập vào hệ thống trước
        clientManager.registerUser(userId, mockOut1);

        // Đóng vai trò AuctionManager tự động đẩy người trả giá cao nhất vào phòng đấu giá
        clientManager.subscribeUserToAuction(auctionId, userId);

        // Phát tin nhắn tới phòng và kiểm tra tính hợp lệ
        clientManager.broadcastEventToAuction(auctionId, mockEvent);
        verify(mockOut1, times(1)).writeObject(mockEvent);

        // Người này bị outbid (bị trả giá cao hơn), tự động hủy khỏi phòng đấu giá
        clientManager.unsubscribeUserFromAuction(auctionId, userId);
        clientManager.broadcastEventToAuction(auctionId, mockEvent);
        
        // Luồng nhận tin nhắn không tăng thêm
        verify(mockOut1, times(1)).writeObject(mockEvent);
    }

    @Test
    public void testBroadcastSystemEventToAllClients() throws IOException {
        // Thêm nhiều client khác nhau vào danh sách hệ thống công cộng
        clientManager.addClient(mockOut1);
        clientManager.addClient(mockOut2);

        // Phát sự kiện toàn hệ thống (Ví dụ: Bảo trì hệ thống hoặc cập nhật trang chủ)
        clientManager.broadcastSystemEvent(mockEvent);

        // Đảm bảo tất cả mọi người đang kết nối đều nhận được tin
        verify(mockOut1, times(1)).writeObject(mockEvent);
        verify(mockOut2, times(1)).writeObject(mockEvent);
    }

    @Test
    public void testRemoveClientCompletelyCleanup() throws IOException {
        int auctionId = 5005;
        clientManager.addClient(mockOut1);
        clientManager.addAdminClient(mockOut1);
        clientManager.subscribeToAuction(auctionId, mockOut1);

        // Client mất kết nối đột ngột -> Tiến hành quét dọn toàn diện tài nguyên
        clientManager.removeClientCompletely(mockOut1);

        // Kiểm tra xem đã dọn sạch ở mọi danh mục lưu trữ chưa
        assertEquals(0, clientManager.getClients().size());
        
        // Thử phát tin nhắn tới các phòng vừa rồi, client bị xóa không được nhận gì nữa
        clientManager.broadcastToAdmins(mockEvent);
        clientManager.broadcastEventToAuction(auctionId, mockEvent);
        
        verify(mockOut1, never()).writeObject(any());
    }

    @Test
    public void testSendEventSafelyHandlesExceptionRobustly() throws IOException {
        clientManager.addClient(mockOut1);

        // Giả lập kịch bản đường truyền mạng của client này bị đứt (quăng lỗi IOException)
        doThrow(new IOException("Đứt cáp kết nối đột ngột")).when(mockOut1).writeObject(any());

        // Hệ thống phát tin nhắn đi, khối try-catch của sendEventSafely phải hoạt động 
        // để nuốt lỗi êm đẹp, giữ cho Server không bị crash ứng dụng giữa chừng.
        assertDoesNotThrow(() -> clientManager.broadcastSystemEvent(mockEvent));
    }
}