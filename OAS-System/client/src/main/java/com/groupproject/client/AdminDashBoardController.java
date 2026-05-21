package com.groupproject.client;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.enums.AuctionStatus;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class AdminDashBoardController {
    @FXML private Button btnAll, btnWaiting, btnActived, btnEnded, btnCancelled, btnFinished;
    @FXML private Label lblPageTitle;
    @FXML private TableView<Auction> adminTableView;
    private ObservableList<Auction> masterAuctionList = FXCollections.observableArrayList();
    private FilteredList<Auction> filteredAuctionList = new FilteredList<>(masterAuctionList, p -> true);
    // Tấm bản đồ cấu hình bộ lọc
    private final Map<Button, Predicate<Auction>> filterConfigMap = new HashMap<>();

    @FXML
    public void initialize() {
        adminTableView.setItems(filteredAuctionList);
        setupFilterRules();
        
        // Mặc định ban đầu hiển thị tất cả
        btnAll.requestFocus(); 
        // (Tùy chọn) Gửi Request lên Server để lấy dữ liệu đổ vào masterAuctionList tại đây
        // loadDataFromServer();

    }

    /**
     * NƠI ĐỊNH NGHĨA QUY TẮC: Không dùng if-else, dùng Map điền cấu hình
     */
    private void setupFilterRules() {
        // 1. Nút "Tất cả" -> Luôn trả về true (không lọc gì cả)
        filterConfigMap.put(btnAll, auction -> true);
        
        // 2. Nút "Chờ duyệt" -> Chỉ lấy các phiên WAITING
        filterConfigMap.put(btnWaiting, auction -> auction.getStatus() == AuctionStatus.WAITING);
        
        // 3. Nút "Đang diễn ra" -> Chỉ lấy các phiên ACTIVED (hoặc ACTIVE tùy enum của bạn)
        filterConfigMap.put(btnActived, auction -> auction.getStatus() == AuctionStatus.ACTIVED);
        
        // 4. Nút "Đã kết thúc" -> Chỉ lấy các phiên 
        filterConfigMap.put(btnEnded, auction -> auction.getStatus() == AuctionStatus.ENDED);
        
        // 5. Nút "Đã hủy" -> Chỉ lấy các phiên CANCELLED
        filterConfigMap.put(btnCancelled, auction -> auction.getStatus() == AuctionStatus.CANCELLED);

        //6.  Nút "Đã hoàn thành" -> Chỉ lấy các phiên
        filterConfigMap.put(btnFinished, auction -> auction.getStatus() == AuctionStatus.FINISHED);
    }

    /**
     * HÀM XỬ LÝ SỰ KIỆN TẬP TRUNG (ZERO IF-ELSE)
     */
    @FXML
    private void handleFilterAction(ActionEvent event) {
        // Xác định chính xác Nút nào vừa được Admin click vào
        Button clickedButton = (Button) event.getSource();
        
        // 1. Lấy ra quy tắc lọc tương ứng từ Map cấu hình đã dựng sẵn
        Predicate<Auction> selectedPredicate = filterConfigMap.get(clickedButton);
        
        if (selectedPredicate != null) {
            // 2. Kích hoạt bộ lọc cho FilteredList (TableView sẽ tự động cập nhật dưới mắt admin)
            filteredAuctionList.setPredicate(selectedPredicate);
            
            // 3. Thay đổi tiêu đề trang bằng chính Text của nút đó một cách tự động
            lblPageTitle.setText(clickedButton.getText());
            
            // 4. Tiện ích UX: Đổi màu nút đang chọn để Admin biết mình đang ở đâu
            highlightSelectedButton(clickedButton);
        }
    }
    private void highlightSelectedButton(Button activeRow) {
        // Reset style của tất cả các nút về mặc định
        filterConfigMap.keySet().forEach(btn -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;"));
        
        // Riêng nút được bấm thì đổi màu highlight nổi bật lên (Ví dụ màu xanh tươi)
        activeRow.setStyle("-fx-background-color: #34495e; -fx-text-fill: #3498db; -fx-font-weight: bold;");
    }
}
