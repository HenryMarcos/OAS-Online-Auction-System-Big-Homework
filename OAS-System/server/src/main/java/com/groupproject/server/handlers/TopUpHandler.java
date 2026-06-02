package com.groupproject.server.handlers;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.requests.TopUpRequest;
import com.groupproject.shared.network.responses.Response;
import com.groupproject.shared.network.responses.TopUpResponse;

public class TopUpHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        ServerLogger.info("Handling " + request.getClass().getSimpleName());

        if (!(request instanceof TopUpRequest)) {
            ServerLogger.warning("Invalid request type for TopUpHandler: " + request.getClass().getSimpleName());
            return new TopUpResponse(false, "Invalid request type.");
        }

        Integer userId = clientContext.getAuthenticatedUserId();
        if (userId == null) {
            ServerLogger.warning("TopUp rejected: User is not authenticated.");
            return new TopUpResponse(false, "Bạn chưa đăng nhập.");
        }

        TopUpRequest topUpRequest = (TopUpRequest) request;
        double amount = topUpRequest.getAmount();

        if (amount <= 0) {
            ServerLogger.warning("TopUp rejected: Invalid amount $" + amount + " from user " + userId);
            return new TopUpResponse(false, "Số tiền nạp phải lớn hơn 0.");
        }

        boolean success = UserDAO.addBalance(userId, amount);
        if (success) {
            double newBalance = UserDAO.getBalance(userId);
            ServerLogger.info("User " + userId + " topped up $" + amount + ". New balance: $" + newBalance);
            return new TopUpResponse(true, "Nạp tiền thành công! Số dư mới: $" + newBalance, newBalance);
        } else {
            ServerLogger.error("TopUp failed for user " + userId);
            return new TopUpResponse(false, "Nạp tiền thất bại. Vui lòng thử lại.");
        }
    }
}
