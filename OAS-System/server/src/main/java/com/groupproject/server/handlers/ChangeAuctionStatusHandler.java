package com.groupproject.server.handlers;

import java.time.Duration;
import java.time.LocalDateTime;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.service.AuctionManager; // Thêm import này để check Admin
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.request.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.response.Response;

public class ChangeAuctionStatusHandler implements RequestHandler {

    private static final long MIN_SECONDS_BEFORE_END_TO_START = 30; // Ngưỡng tối thiểu để bắt đầu đấu giá

    @Override
    public Response handle(Request request) {
        if (!(request instanceof ChangeAuctionStatusRequest)) {
            return new ChangeAuctionStatusResponse(false, "Invalid request type");
        }

        ChangeAuctionStatusRequest changeReq = (ChangeAuctionStatusRequest) request;
        int auctionId = changeReq.getAuctionId();
        AuctionStatus newStatus = changeReq.getNewStatus();
        
        // ==============================================================
        // BƯỚC BẢO MẬT: LẤY THÔNG TIN NGƯỜI DÙNG TỪ BỘ NHỚ SERVER
        // ==============================================================
        User userMakingRequest = ClientContext.currentUser.get();
        
        // 1. Nếu chưa đăng nhập (hoặc phiên hết hạn)
        if (userMakingRequest == null) {
            return new ChangeAuctionStatusResponse(false, "Bạn chưa đăng nhập hoặc phiên kết nối đã hết hạn!");
        }

        int requesterId = userMakingRequest.getId();
        boolean isAdmin = userMakingRequest.isAdmin();

        // 1. Lấy thông tin hiện tại từ DB để đối chiếu
        Auction currentAuction = AuctionDAO.getAuctionById(auctionId);
        if (currentAuction == null) {
            return new ChangeAuctionStatusResponse(false, "Phiên đấu giá không tồn tại trong hệ thống.");
        }
        if (currentAuction.getSellerId() != requesterId && !isAdmin) {
            ServerLogger.warning("CẢNH BÁO BẢO MẬT: User ID " + requesterId + " đang cố gắng can thiệp vào Auction ID " + auctionId);
            return new ChangeAuctionStatusResponse(false, "Từ chối truy cập: Bạn không phải là chủ sở hữu của phiên đấu giá này.");
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
            
            // Xử lý an toàn: Đề phòng endTime bị null văng lỗi Crash Server
            if (currentAuction.getEndTime() == null) {
                return new ChangeAuctionStatusResponse(false, "Phiên đấu giá bị lỗi dữ liệu (Không có thời gian kết thúc).");
            }

            long secondsRemaining = Duration.between(now, currentAuction.getEndTime()).toSeconds();

            // Nếu sát giờ hoặc lố giờ -> Ép kết thúc
            if (secondsRemaining < MIN_SECONDS_BEFORE_END_TO_START) {
                ServerLogger.warning("Auction " + auctionId + " start request too close to end_time. Forcing ENDED.");
                if (AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.ENDED)) {
                    AuctionManager.getInstance().forceCancelWaitingAuction(auctionId); // Gỡ khỏi RAM
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