package com.groupproject.server.handlers;

import java.time.Duration;
import java.time.LocalDateTime;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

public class ChangeAuctionStatusHandler implements RequestHandler {

    @Override
    public Response handle(Request request) {
        if (!(request instanceof ChangeAuctionStatusRequest)) {
            return new ChangeAuctionStatusResponse(false, "Invalid request type");
        }

        ChangeAuctionStatusRequest changeReq = (ChangeAuctionStatusRequest) request;
        int auctionId = changeReq.getAuctionId();
        AuctionStatus newStatus = changeReq.getNewStatus();

        // 1. Lấy thông tin hiện tại từ DB để đối chiếu
        Auction currentAuction = AuctionDAO.getAuctionById(auctionId);
        if (currentAuction == null) {
            return new ChangeAuctionStatusResponse(false, "Phiên đấu giá không tồn tại trong hệ thống.");
        }

        AuctionStatus currentStatus = currentAuction.getStatus();

        // ==========================================
        // NHÁNH 1: XỬ LÝ NÚT "START NOW" (ACTIVED)
        // ==========================================
        if (newStatus == AuctionStatus.ACTIVED) {
            if (currentStatus != AuctionStatus.WAITING && currentStatus != AuctionStatus.SCHEDULED) {
                return new ChangeAuctionStatusResponse(false, "Chỉ có thể bắt đầu phiên đang ở trạng thái WAITING hoặc SCHEDULED.");
            }

            LocalDateTime now = LocalDateTime.now();
            long secondsRemaining = Duration.between(now, currentAuction.getEndTime()).toSeconds();

            // Nếu sát giờ hoặc lố giờ -> Ép kết thúc
            if (secondsRemaining < 60) {
                ServerLogger.warning("Auction " + auctionId + " start request too close to end_time. Forcing ENDED.");
                if (AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.ENDED)) {
                    AuctionManager.getInstance().cancelAuction(auctionId); // Gỡ khỏi RAM
                    currentAuction.setStatus(AuctionStatus.ENDED);
                    return new ChangeAuctionStatusResponse(false, "Đã quá sát giờ kết thúc, phiên đấu giá bị đóng tự động.", currentAuction);
                }
            }

            // Bắt đầu bình thường
            if (AuctionDAO.updateAuctionStatus(auctionId, newStatus, now)) {
                Auction updatedAuction = AuctionDAO.getAuctionById(auctionId);
                AuctionManager.getInstance().activateWaitingAuction(updatedAuction);
                return new ChangeAuctionStatusResponse(true, "Bắt đầu phiên đấu giá thành công!", updatedAuction);
            }
        } 
        
        // ==========================================
        // NHÁNH 2: XỬ LÝ NÚT "HỦY ĐẤU GIÁ" (CANCELLED)
        // ==========================================
        else if (newStatus == AuctionStatus.CANCELLED) {
            // Không cho phép hủy nếu đã kết thúc hoặc đã hủy từ trước
            if (currentStatus == AuctionStatus.ENDED || currentStatus == AuctionStatus.CANCELLED) {
                return new ChangeAuctionStatusResponse(false, "Phiên đấu giá này đã kết thúc hoặc đã bị hủy.");
            }

            // Cập nhật Database
            boolean isCancelled = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.CANCELLED);
            
            if (isCancelled) {
                // Xóa khỏi RAM để ngừng đếm ngược
                AuctionManager.getInstance().cancelAuction(auctionId);
                
                // Trả về cho Client
                currentAuction.setStatus(AuctionStatus.CANCELLED);
                return new ChangeAuctionStatusResponse(true, "Đã hủy phiên đấu giá thành công!", currentAuction);
            }
        } 
        
        // ==========================================
        // NHÁNH 3: TRƯỜNG HỢP KHÁC
        // ==========================================
        else {
            return new ChangeAuctionStatusResponse(false, "Request không hỗ trợ đổi sang trạng thái này.");
        }

        return new ChangeAuctionStatusResponse(false, "Có lỗi xảy ra khi cập nhật Database.");
    }
}