package com.groupproject.server.handlers;

import java.time.LocalDateTime;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.responses.Response;

public class ChangeAuctionStatusHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler clientContext) {
        if (!(request instanceof ChangeAuctionStatusRequest)) {
            return new ChangeAuctionStatusResponse(false, "Invalid request type");
        }

        ChangeAuctionStatusRequest changeReq = (ChangeAuctionStatusRequest) request;
        int auctionId = changeReq.getAuctionId();
        AuctionStatus newStatus = changeReq.getNewStatus();

        Auction currentAuction = AuctionDAO.getAuctionById(auctionId);
        if (currentAuction == null) {
            return new ChangeAuctionStatusResponse(false, "Phiên đấu giá không tồn tại trong hệ thống.");
        }

        AuctionStatus currentStatus = currentAuction.getStatus();

        // Chỉ seller hoặc admin mới được thao tác
        Integer requestingUserId = clientContext.getAuthenticatedUserId();
        boolean isAdmin = (requestingUserId != null && requestingUserId == 999999);
        if (!isAdmin && (requestingUserId == null || requestingUserId != currentAuction.getSellerId())) {
            return new ChangeAuctionStatusResponse(false, "Bạn không có quyền thao tác phiên đấu giá này.");
        }

        if (newStatus == AuctionStatus.ACTIVATED) {
            if (currentStatus != AuctionStatus.WAITING && currentStatus != AuctionStatus.SCHEDULED) {
                return new ChangeAuctionStatusResponse(false, "Chỉ có thể bắt đầu phiên đang ở trạng thái WAITING hoặc SCHEDULED.");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusSeconds(currentAuction.getDuration());
            
            if (AuctionDAO.updateAuctionStatusWithTime(auctionId, newStatus, now, endTime)) {
                Auction updatedAuction = AuctionDAO.getAuctionById(auctionId);
                AuctionManager.INSTANCE.activateWaitingAuction(updatedAuction);
                return new ChangeAuctionStatusResponse(true, "Bắt đầu phiên đấu giá thành công!", updatedAuction);
            }
        } 
        else if (newStatus == AuctionStatus.CANCELLED) {
            if (currentStatus == AuctionStatus.ENDED || currentStatus == AuctionStatus.CANCELLED) {
                return new ChangeAuctionStatusResponse(false, "Phiên đấu giá này đã kết thúc hoặc đã bị hủy.");
            }

            boolean isCancelled = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELLED);
            if (isCancelled) {
                AuctionManager.INSTANCE.cancelAuction(auctionId); 
                currentAuction.setStatus(AuctionStatus.CANCELLED);
                return new ChangeAuctionStatusResponse(true, "Đã hủy phiên đấu giá thành công!", currentAuction);
            }
        } 
        
        return new ChangeAuctionStatusResponse(false, "Có lỗi xảy ra khi cập nhật Database.");
    }
}
