package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestDispatcherTest {

    private RequestDispatcher dispatcher;
    private ClientHandler mockClientContext;
    private RequestHandler mockHandler;
    private Map<Class<? extends Request>, RequestHandler> internalHandlersMap;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        dispatcher = new RequestDispatcher();
        mockClientContext = mock(ClientHandler.class);
        mockHandler = mock(RequestHandler.class);

        // Lấy Map handlers bên trong lớp bằng Reflection để cấu hình biệt lập
        Field field = RequestDispatcher.class.getDeclaredField("handlers");
        field.setAccessible(true);
        internalHandlersMap = (Map<Class<? extends Request>, RequestHandler>) field.get(dispatcher);
    }

    @Test
    @DisplayName("Test Dispatch thành công khi Request đã được đăng ký Handler")
    void testDispatchSuccess() {
        LoginRequest mockRequest = mock(LoginRequest.class);
        Response mockResponse = mock(Response.class);
        
        // Đăng ký mock handler cho LoginRequest trong môi trường test
        internalHandlersMap.put(LoginRequest.class, mockHandler);
        when(mockHandler.handle(mockRequest, mockClientContext)).thenReturn(mockResponse);

        Response actualResponse = dispatcher.dispatch(mockRequest, mockClientContext);

        assertNotNull(actualResponse);
        assertEquals(mockResponse, actualResponse);
        verify(mockHandler, times(1)).handle(mockRequest, mockClientContext);
    }

    @Test
    @DisplayName("Test Dispatch trả về null khi Request không có Handler phù hợp")
    void testDispatchNoHandlerFound() {
        // Tạo một class Request vô danh không nằm trong danh sách đăng ký
        Request unknownRequest = mock(Request.class);

        Response actualResponse = dispatcher.dispatch(unknownRequest, mockClientContext);

        assertNull(actualResponse, "Nếu không tìm thấy handler thì phải trả về null");
    }
}