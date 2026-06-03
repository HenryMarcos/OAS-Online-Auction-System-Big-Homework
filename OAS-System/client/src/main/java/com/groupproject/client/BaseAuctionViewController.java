package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.responses.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.responses.CreateAuctionResponse;
import com.groupproject.shared.network.responses.GetAuctionResponse;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;


// Màn hình sẽ được áp dụng trong HOME, ACTION LISTINGS VÀ MY AUCTIONS
public abstract class BaseAuctionViewController implements Initializable, LifecycleController {
   @FXML protected ScrollPane scrollPane;
   @FXML protected  GridPane productgrid;
   @FXML protected  Button sortbutton;
   @FXML protected Button activeCategoryButton;
   @FXML protected  TreeView<Category> categoryTreeView;
   protected ObservableList<Auction> uiList = FXCollections.observableArrayList();
   protected List<LifecycleController> childControllers = new ArrayList<>();

   private final Consumer<CreateAuctionResponse> createAuctionListener = this::handleCreateResponse;
   private final Consumer<GetAuctionResponse> getAuctionListener = this::handleGetResponse;
   private final Consumer<ChangeAuctionStatusResponse> changeStatusListener = this::handleChangeStatusResponse;

   // man hinh moi khi an vao nut sortby
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Only the parent handles initialization now!
        addEventHandles();
        fetchInitialData();
    }

    // --- NEW: Safe Data Injection ---
    public void setAuctionsData(List<Auction> newData) {
        Platform.runLater(() -> {
            // 1. Clean up old memory
            for (LifecycleController child : childControllers) {
                child.cleanup();
            }
            childControllers.clear();
            uiList.clear();

            // 2. Filter and add new data
            if (newData != null) {
                for (Auction a : newData) {
                    if (shouldInclude(a)) uiList.add(a);
                }
            }
            renderGrid();
        });
    }

    public void renderGrid() {
        productgrid.getChildren().clear();
        int maxColumns = getMaxColumns();
        for (int i = 0; i < uiList.size(); i++) {
            Auction auction = uiList.get(i);
            Node cardNode = createCardNode(auction);
            if (cardNode != null) {
                productgrid.add(cardNode, i % maxColumns, i / maxColumns);
                GridPane.setMargin(cardNode, new Insets(10));
            }
        }
    }

    public void makeSmoothScrolling(ScrollPane scrollPane) {
      // Tốc độ cuộn: Số càng lớn cuộn càng nhanh. (Chuẩn web thường quanh mức 0.005)
        final double SCROLL_SPEED = 0.005; 

        scrollPane.setOnScroll(event -> {
            // 1. Chặn ngay lập tức cú giật cuộn mặc định của JavaFX
            event.consume();
            
            // 2. Lấy vị trí thanh cuộn hiện tại (nằm trong khoảng 0.0 ở đỉnh đến 1.0 ở đáy)
            double currentVValue = scrollPane.getVvalue();
            
            // 3. Tính toán vị trí mới dựa vào hướng lăn chuột (deltaY)
            double targetVValue = currentVValue - (event.getDeltaY() * SCROLL_SPEED);
            
            // 4. Giới hạn lại để thanh cuộn không bị văng ra khỏi mốc 0.0 và 1.0
            targetVValue = Math.max(0.0, Math.min(1.0, targetVValue));
            
            // 5. Tạo hiệu ứng trượt êm ái trong 250 mili-giây (Dùng Interpolator.EASE_OUT để trượt chậm dần lúc dừng)
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(250), 
                new KeyValue(scrollPane.vvalueProperty(), targetVValue, javafx.animation.Interpolator.EASE_OUT))
            );
            
            timeline.play();
        });
    }

    public void addEventHandles()  {
        if (sortbutton != null) {
            sortbutton.setOnMouseClicked(mouseEvent -> {
                Stage stage = new Stage();
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(App.class.getResource("/com/groupproject/client/FXML/sortmenu.fxml"));
                try {
                    AnchorPane root = loader.load();
                    stage.setScene(new Scene(root));
                    stage.initStyle(StageStyle.TRANSPARENT);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    public void setupTreeViewConfiguration() {
        // Đặt CellFactory để ép TreeView hiển thị getName() thay vì gọi toString() mặc định của Object
        categoryTreeView.setCellFactory(tv -> new TreeCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        // Lắng nghe sự kiện khi người dùng chọn một mục trên Cây
        categoryTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Category selectedCategory = newValue.getValue();
                
                // Nếu người dùng click vào nút gốc "Tất cả" (ID là "-1")
                if ("-1".equals(selectedCategory.getId())) {
                    fetchInitialData(); // Gọi Server lấy hết toàn bộ Auction
                } else {
                    // Gọi Server lấy các Auction thuộc danh mục cụ thể này
                    fetchDataByCategory(selectedCategory.getId()); 
                }
            }
        });
   }

    /**
     * 2. Hàm tiếp nhận dữ liệu từ Server gửi về để vẽ lên cây
     */
    public void drawCategoryTreeUI(List<Category> serverCategories) {
        // Tạo một Node Gốc ảo có tên là "Tất cả danh mục" để người dùng click khi muốn xem lại hết sản phẩm
        // Chỗ này sẽ xử lý sau 
        Category allCategoryMarker = new Category(-1, "Tất cả danh mục",null);
        TreeItem<Category> rootItem = new TreeItem<>(allCategoryMarker);
        rootItem.setExpanded(true); // Luôn mở bung nút gốc này ra

        // Gọi hàm đệ quy để đắp các nhánh con từ danh sách Server gửi về
        buildTreeRecursive(rootItem, serverCategories);

        // Đổ toàn bộ cây vào giao diện
        categoryTreeView.setRoot(rootItem);
        
        // Bạn có thể ẩn/hiển thị nút Root này tùy ý (ở đây chọn TRUE để người dùng bấm được vào chữ "Tất cả danh mục")
        categoryTreeView.setShowRoot(true); 
    }

    /**
     * 3. THUẬT TOÁN ĐỆ QUY: Tự động đào sâu xuống mọi cấp độ danh mục để tạo nhánh
     */
    public void buildTreeRecursive(TreeItem<Category> parentNode, List<Category> subCategories) {
        if (subCategories == null || subCategories.isEmpty()) {
            return;
        }

        for (Category cat : subCategories) {
            // Tạo một nút con mới cho cây
            TreeItem<Category> childNode = new TreeItem<>(cat);
            parentNode.getChildren().add(childNode);

            // Nếu danh mục này tiếp tục đẻ ra con (Cấp 3, Cấp 4...) -> Tiếp tục đào sâu xuống
            if (cat.getSubCategories() != null && !cat.getSubCategories().isEmpty()) {
                childNode.setExpanded(false); // Mặc định thu gọn các nhánh con cho gọn gàng
                buildTreeRecursive(childNode, cat.getSubCategories());
            }
        }
    }
    public void setupReactiveUI() {
        uiList.addListener((ListChangeListener<Auction>) change -> {
            Platform.runLater(this::renderGrid);
        });
    }
    
    public void registerChildController(LifecycleController controller) {
        if (controller != null) {
            childControllers.add(controller);
        }
    }

    public Node createCardNode(Auction auction) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/card.fxml"));
            Node node = loader.load();
            CardController ctrl = loader.getController();
            ctrl.setAuction(auction);
            childControllers.add(ctrl);
            return node;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setupGlobalEventListeners() {
        ClientMessageRouter.INSTANCE.onResponse(CreateAuctionResponse.class, createAuctionListener);
        ClientMessageRouter.INSTANCE.onResponse(GetAuctionResponse.class, getAuctionListener);
        ClientMessageRouter.INSTANCE.onResponse(ChangeAuctionStatusResponse.class, changeStatusListener);
    }

    private void handleCreateResponse(CreateAuctionResponse response) {
        if (response.isSuccess()) {
            Auction newAuction = response.getAuction();
            if (shouldInclude(newAuction)) {
                Platform.runLater(() -> uiList.add(0, newAuction));
            }
        }
    }

    private void handleGetResponse(GetAuctionResponse response) {
        if (response.isSuccess()) {
            List<Auction> serverAuctions = response.getAuctions();
            List<Auction> filteredAuctions = serverAuctions.stream()
                .filter(this::shouldInclude) 
                .toList();
            Platform.runLater(() -> {
                uiList.setAll(filteredAuctions); 
            });
        } else {
            ClientLogger.error("Không thể lấy danh sách phiên đấu giá từ Server");
        }
    }

    private void handleChangeStatusResponse(com.groupproject.shared.network.responses.ChangeAuctionStatusResponse response) {
        if (response.isSuccess()) {
            Platform.runLater(this::fetchInitialData);
        }
    }

    @Override
    public void cleanup() {
        ClientMessageRouter.INSTANCE.offResponse(CreateAuctionResponse.class, createAuctionListener);
        ClientMessageRouter.INSTANCE.offResponse(GetAuctionResponse.class, getAuctionListener);
        ClientMessageRouter.INSTANCE.offResponse(ChangeAuctionStatusResponse.class, changeStatusListener);
        for (LifecycleController child : childControllers) {
            child.cleanup();
        }
        childControllers.clear();
    }
    protected int getMaxColumns() {
        return 3;
    }

    public void highlightCategoryButton(Button clickedButton) {
        if (activeCategoryButton != null ) {
            activeCategoryButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #333333;");
        }
        clickedButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-weight: bold;");
        this.activeCategoryButton= clickedButton;
    }
    // BA HÀM NÀY NÊN ĐƯỢC ĐẨY VỀ TRONG PHẦN MAINCONTROLLER ĐỂ XỬ LÝ 
    // HÀM NÀY DUYỆT XEM ĐIỀU KIỆN NÀO THÌ ĐƯỢC XUẤT HIỆN TRÊN MÀN HÌNH CỤ THỂ
    abstract  boolean shouldInclude(Auction item);
    // HÀM NÀY SẼ BAO GỒM VIỆC GỬI REQUEST LÊN SERVER VỚI ĐIỀU KIỆN ĐÃ ĐƯỢC THỐNG NHẤT TRONG shouldInclude(Auction item)
    abstract void fetchInitialData();
    // LẤY AUCTIONS THEO Id của Category
    abstract void fetchDataByCategory(int categoryId);
    // NEW: Children must define where they pull session data from
    public abstract void refreshFromSession();
}
