package com.groupproject.server.core;

import com.groupproject.server.handlers.RequestDispatcher;
import com.groupproject.shared.network.requests.GetAuctionRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.Response;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    @Test
    public void testClientHandlerRunAndDisconnect() throws Exception {
        // ------------------------------------------------------------------
        // 1. CHUẨN BỊ MÔI TRƯỜNG MẠNG GIẢ (MOCK SOCKET STREAMS)
        // ------------------------------------------------------------------
        
        // Stream đầu ra của server (Dùng để hứng dữ liệu Server định gửi về Client)
        ByteArrayOutputStream serverOutputStream = new ByteArrayOutputStream();

        // Stream đầu vào từ client (Mô phỏng Client gửi dữ liệu lên Server)
        // Ta phải đóng gói 1 object thật vào ByteArray để ObjectInputStream đọc không bị lỗi Header
        ByteArrayOutputStream fakeClientData = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(fakeClientData);
        oos.writeObject(new GetAuctionRequest()); // Mô phỏng client gửi GetAuctionRequest
        oos.flush();
        
        // Đưa mảng byte đã đóng gói vào InputStream của Server
        // Lưu ý: Sau khi đọc xong object đầu tiên, luồng sẽ cạn và quăng EOFException, 
        // điều này giúp tự động phá vỡ vòng lặp while(true) trong ClientHandler.run()
        ByteArrayInputStream serverInputStream = new ByteArrayInputStream(fakeClientData.toByteArray());

        // ------------------------------------------------------------------
        // 2. GIẢ LẬP SOCKET
        // ------------------------------------------------------------------
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.getOutputStream()).thenReturn(serverOutputStream);
        when(mockSocket.getInputStream()).thenReturn(serverInputStream);
        when(mockSocket.isClosed()).thenReturn(false);
        
        // Giả lập IP mạng để không bị NullPointerException khi class gọi in log
        InetAddress mockInetAddress = mock(InetAddress.class);
        when(mockInetAddress.toString()).thenReturn("/127.0.0.1");
        when(mockSocket.getInetAddress()).thenReturn(mockInetAddress);

        // Giả lập Response trả về từ Server
        Response mockResponse = mock(Response.class);

        // ------------------------------------------------------------------
        // 3. GIẢ LẬP RequestDispatcher (Xử lý đối tượng được khởi tạo bằng 'new')
        // ------------------------------------------------------------------
        // Vì RequestDispatcher được new thẳng trong hàm run(), ta dùng MockedConstruction để can thiệp
        try (MockedConstruction<RequestDispatcher> mockedDispatcher = mockConstruction(RequestDispatcher.class,
                (mock, context) -> {
                    // Khi Dispatcher giả được gọi, ép nó trả về mockResponse
                    when(mock.dispatch(any(Request.class), any(ClientHandler.class))).thenReturn(mockResponse);
                })) {

            // ------------------------------------------------------------------
            // 4. KHỞI TẠO VÀ CHẠY THỬ
            // ------------------------------------------------------------------
            ClientHandler handler = new ClientHandler(mockSocket);
            
            // Gán user giả để test đoạn code unregisterUser trong khối finally
            handler.setAuthenticatedUserId(99); 
            assertEquals(99, handler.getAuthenticatedUserId());

            // Chạy luồng run()
            // Quá trình: Đọc GetAuctionRequest -> Xử lý Dispatcher -> Quay lại while -> Lỗi hết Data -> Vào Finally dọn dẹp
            assertDoesNotThrow(() -> handler.run());

            // ------------------------------------------------------------------
            // 5. KIỂM TRA KẾT QUẢ ĐẠT ĐƯỢC (VERIFY)
            // ------------------------------------------------------------------
            // Đảm bảo Output Stream của Handler đã được tạo thành công
            assertNotNull(handler.getOut());

            // Kiểm tra xem socket.close() có thực sự được gọi ở cuối hàm (khối finally) không
            verify(mockSocket, times(1)).close();

            // Kiểm tra xem Dispatcher có thực sự bắt được Request và xử lý không
            assertEquals(1, mockedDispatcher.constructed().size());
            RequestDispatcher dispatcher = mockedDispatcher.constructed().get(0);
            verify(dispatcher, times(1)).dispatch(any(Request.class), eq(handler));
        }
    }
}