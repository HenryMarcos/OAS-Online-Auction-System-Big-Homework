package com.groupproject.server.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedConstruction;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerAppTest {

    private ObjectOutputStream mockSender;
    private ObjectOutputStream mockReceiver1;
    private ObjectOutputStream mockReceiver2;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        mockSender = mock(ObjectOutputStream.class);
        mockReceiver1 = mock(ObjectOutputStream.class);
        mockReceiver2 = mock(ObjectOutputStream.class);

        // Sử dụng Reflection để dọn sạch danh sách clients cũ trong ClientManager Singleton
        Field clientsField = ClientManager.class.getDeclaredField("clients");
        clientsField.setAccessible(true);
        List<ObjectOutputStream> clients = (List<ObjectOutputStream>) clientsField.get(ClientManager.INSTANCE);
        clients.clear();
    }

    @Test
    public void testBroadcastSuccess() throws IOException {
        // 1. Đăng ký các luồng ra dữ liệu vào ClientManager hệ thống
        ClientManager.INSTANCE.addClient(mockSender);
        ClientManager.INSTANCE.addClient(mockReceiver1);
        ClientManager.INSTANCE.addClient(mockReceiver2);

        String message = "Tin nhắn test hệ thống công cộng";

        // 2. Kích hoạt tính năng broadcast phát tin từ ServerApp
        ServerApp.broadcast(message, mockSender);

        // 3. Kiểm tra tính chính xác: Tất cả mọi người PHẢI nhận được tin nhắn
        verify(mockReceiver1, times(1)).writeObject(message);
        verify(mockReceiver1, times(1)).flush();
        verify(mockReceiver2, times(1)).writeObject(message);
        verify(mockReceiver2, times(1)).flush();

        // 🌟 KIỂM TRA QUAN TRỌNG: Chính người gửi (mockSender) KHÔNG ĐƯỢC nhận lại tin của mình
        verify(mockSender, never()).writeObject(anyString());
    }

    @Test
    public void testBroadcastHandlesExceptionRobustly() throws IOException {
        ClientManager.INSTANCE.addClient(mockSender);
        ClientManager.INSTANCE.addClient(mockReceiver1);

        // Giả lập tình huống máy khách receiver1 đột ngột mất mạng (quăng lỗi IOException)
        doThrow(new IOException("Đứt kết nối socket")).when(mockReceiver1).writeObject(anyString());

        // Khối xử lý try-catch bên trong vòng lặp broadcast phải tự cô lập lỗi, 
        // không cho phép một client lỗi làm sập toàn bộ tiến trình broadcast của hệ thống.
        assertDoesNotThrow(() -> ServerApp.broadcast("Hello World", mockSender));
    }

    @Test
    public void testMainServerLoopLifecycle() {
        // Giả lập một kết nối socket từ client truyền vào
        Socket mockClientSocket = mock(Socket.class);

        // Sử dụng mockConstruction để can thiệp trực tiếp vào hành vi "new ServerSocket" và "new ClientHandler"
        try (MockedConstruction<ServerSocket> mockedServerSocket = mockConstruction(ServerSocket.class,
                (mockServer, context) -> {
                    // 🌟 CHIẾN THUẬT BẺ GÃY VÒNG LẶP VÔ HẠN:
                    // Lần 1: accept() trả về client socket giả lập thành công để đi vào luồng xử lý ThreadPool.
                    // Lần 2: accept() chủ động quăng lỗi IOException nhằm phá vỡ vòng lặp while(true) ngay lập tức.
                    when(mockServer.accept())
                            .thenReturn(mockClientSocket)
                            .thenThrow(new IOException("Chủ động kết thúc vòng lặp để hoàn thành Unit Test"));
                });
             // Giả lập ClientHandler để ThreadPool thực thi một hàm rỗng (no-op), tránh chiếm dụng tài nguyên máy
             MockedConstruction<ClientHandler> mockedHandler = mockConstruction(ClientHandler.class)) {

            // Chạy hàm main() kiểm thử của ứng dụng Server
            // Lưu ý: Do DatabaseManager và AuctionManager của bạn đều có cấu trúc try-catch nội bộ,
            // khi chạy môi trường test không có DB thật, chúng sẽ in log [ERROR] nhưng KHÔNG làm crash ứng dụng,
            // luồng test vẫn sẽ đi xuống kiểm tra Socket cực kỳ mượt mà.
            assertDoesNotThrow(() -> ServerApp.main(new String[]{}));
        }
    }
}