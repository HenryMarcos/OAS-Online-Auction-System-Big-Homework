package com.groupproject.server.handlers;

import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.request.TopUpRequest;
import com.groupproject.shared.network.response.Response;
import com.groupproject.shared.network.response.TopUpResponse;
import com.groupproject.shared.network.response.ErrorNotLoginResponse;

public class TopUpHandler implements RequestHandler {

    @Override
    public Response handle(Request request) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());
        
        if (!(request instanceof TopUpRequest)) {
            return new TopUpResponse(false, "Invalid request type", 0);
        }

        TopUpRequest topUpReq = (TopUpRequest) request;
        double amount = topUpReq.getAmount();

        // 1. Kiểm tra số tiền nạp
        if (amount <= 0) {
            return new TopUpResponse(false, "Số tiền nạp phải lớn hơn 0.", 0);
        }

        // 2. Lấy thông tin User đang kết nối
        User currentUser = ClientContext.currentUser.get();
        if (currentUser == null) {
            return new ErrorNotLoginResponse("Bạn cần đăng nhập để nạp tiền.");
        }

        // 3. Gọi UserDAO để cộng tiền vào Database
        boolean isSuccess = UserDAO.addBalance(currentUser.getId(), amount);

        // 4. Xử lý kết quả
        if (isSuccess) {
            double newBalance = currentUser.getBalance() + amount;
            
            // Cập nhật lại số dư mới trên RAM để các phiên giao dịch tiếp theo dùng được ngay
            currentUser.setBalance(newBalance);
            
            ServerLogger.info("User ID " + currentUser.getId() + " nạp thành công " + amount + ". Số dư mới: " + newBalance);
            return new TopUpResponse(true, "Nạp tiền thành công!", newBalance);
        } else {
            ServerLogger.error("Lỗi nạp tiền cho User ID " + currentUser.getId());
            return new TopUpResponse(false, "Lỗi hệ thống! Không thể cộng tiền vào tài khoản.", currentUser.getBalance());
        }
    }
}