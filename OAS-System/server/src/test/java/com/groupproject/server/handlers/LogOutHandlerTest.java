package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.shared.network.requests.LogOutRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.LogOutResponse;
import com.groupproject.shared.network.responses.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogOutHandlerTest {

    private LogOutHandler logOutHandler;
    private ClientHandler mockClientContext;

    @BeforeEach
    void setUp() {
        logOutHandler = new LogOutHandler();
        mockClientContext = mock(ClientHandler.class);
    }

    @Test
    @DisplayName("Test Đăng xuất thành công với LogOutRequest hợp lệ")
    void testHandleSuccess() {
        LogOutRequest mockRequest = mock(LogOutRequest.class);

        Response response = logOutHandler.handle(mockRequest, mockClientContext);

        // Kiểm tra xem có set id về null không
        verify(mockClientContext, times(1)).setAuthenticatedUserId(null);
        
        // Kiểm tra response trả về
        assertNotNull(response);
        assertTrue(response instanceof LogOutResponse);
        LogOutResponse logOutResponse = (LogOutResponse) response;
        assertTrue(logOutResponse.isSuccess());
        assertEquals("Successfully log out", logOutResponse.getMessage());
    }

    @Test
    @DisplayName("Test Đăng xuất thất bại khi truyền sai kiểu Request")
    void testHandleInvalidRequestType() {
        // Tạo một request bất kỳ không phải LogOutRequest
        Request invalidRequest = mock(Request.class);

        Response response = logOutHandler.handle(invalidRequest, mockClientContext);

        // Không được phép gọi hàm hủy session giải phóng id
        verify(mockClientContext, never()).setAuthenticatedUserId(any());
        
        assertNotNull(response);
        assertTrue(response instanceof LogOutResponse);
        LogOutResponse logOutResponse = (LogOutResponse) response;
        assertFalse(logOutResponse.isSuccess());
        assertEquals("Failed to log out", logOutResponse.getMessage());
    }
}