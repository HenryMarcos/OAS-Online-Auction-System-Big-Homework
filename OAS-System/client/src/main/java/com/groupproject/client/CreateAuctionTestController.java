package com.groupproject.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.ImageOptimizer;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.responses.CreateAuctionResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

public class CreateAuctionTestController implements Initializable {

    // --- Form fields ---
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private Label mainImageNameLabel;
    @FXML private Label subImagesCountLabel;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField startingPriceField;
    @FXML private VBox dynamicFieldsContainer;
    @FXML private Label statusLabel;
    @FXML private Button submitButton;

    // Lựa chọn chế độ thời gian
    @FXML private ToggleGroup timingToggleGroup;
    @FXML private RadioButton durationRadio;
    @FXML private RadioButton dateRadio;

    // Chế độ Duration
    @FXML private Label durationLabel;
    @FXML private HBox durationInputBox;
    @FXML private Spinner<Integer> daysSpinner;
    @FXML private Spinner<Integer> hoursSpinner;
    @FXML private Spinner<Integer> minsSpinner;

    // Chế độ Schedule (ngày cụ thể)
    @FXML private Label startDateLabel;
    @FXML private HBox startDateBox;
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<Integer> startHourCombo;
    @FXML private ComboBox<Integer> startMinCombo;

    @FXML private Label endDateLabel;
    @FXML private HBox endDateBox;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Integer> endHourCombo;
    @FXML private ComboBox<Integer> endMinCombo;

    private File mainImageFile = null;
    private List<File> subImageFiles = new ArrayList<>();
    private final Map<Integer, Category> allCategoriesMap = new HashMap<>();
    private final Map<String, TextField> dynamicTextFieldsMap = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ClientMessageRouter.INSTANCE.onResponse(CreateAuctionResponse.class, this::handleCreateAuctionResponse);
        setupCategoryComboBoxFormatting();

        List<Category> mainCategories = SessionManager.INSTANCE.getCurrentCategories();
        if (mainCategories != null) populateCategoryData(mainCategories);

        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) generateDynamicFields(newVal);
        });

        setupSpinnersAndCombos();
        setupTimingVisibilityListeners();
        updateTimingFieldsLayout();
    }

    private void setupTimingVisibilityListeners() {
        timingToggleGroup.selectedToggleProperty().addListener((obs, old, newVal) -> updateTimingFieldsLayout());
    }

    private void setupSpinnersAndCombos() {
        daysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 365, 0));
        hoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 1));
        minsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        for (int i = 0; i < 24; i++) { startHourCombo.getItems().add(i); endHourCombo.getItems().add(i); }
        for (int i = 0; i < 60; i += 5) { startMinCombo.getItems().add(i); endMinCombo.getItems().add(i); }

        startHourCombo.getSelectionModel().select(12);
        startMinCombo.getSelectionModel().select(0);
        endHourCombo.getSelectionModel().select(12);
        endMinCombo.getSelectionModel().select(0);
    }

    private void updateTimingFieldsLayout() {
        boolean isDurationMode = durationRadio.isSelected();

        durationLabel.setVisible(isDurationMode);
        durationLabel.setManaged(isDurationMode);
        durationInputBox.setVisible(isDurationMode);
        durationInputBox.setManaged(isDurationMode);

        startDateLabel.setVisible(!isDurationMode);
        startDateLabel.setManaged(!isDurationMode);
        startDateBox.setVisible(!isDurationMode);
        startDateBox.setManaged(!isDurationMode);

        endDateLabel.setVisible(!isDurationMode);
        endDateLabel.setManaged(!isDurationMode);
        endDateBox.setVisible(!isDurationMode);
        endDateBox.setManaged(!isDurationMode);
    }

    // --- Categories ---
    private void populateCategoryData(List<Category> mainCategories) {
        allCategoriesMap.clear();
        categoryComboBox.getItems().clear();
        for (Category mainCat : mainCategories) addCategoryToSelection(mainCat, 0);
    }

    private void addCategoryToSelection(Category cat, int depth) {
        allCategoriesMap.put(cat.getId(), cat);
        categoryComboBox.getItems().add(cat);
        if (cat.getSubCategories() != null)
            for (Category sub : cat.getSubCategories()) addCategoryToSelection(sub, depth + 1);
    }

    private void setupCategoryComboBoxFormatting() {
        categoryComboBox.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                if (category == null) return "";
                StringBuilder prefix = new StringBuilder();
                Category temp = category;
                while (temp.getParentId() != null && allCategoriesMap.containsKey(temp.getParentId())) {
                    prefix.insert(0, "  -> ");
                    temp = allCategoriesMap.get(temp.getParentId());
                }
                return prefix.toString() + category.getName();
            }
            @Override public Category fromString(String string) { return null; }
        });
    }

    private void generateDynamicFields(Category selectedCategory) {
        dynamicFieldsContainer.getChildren().clear();
        dynamicTextFieldsMap.clear();
        if (selectedCategory == null) return;

        List<Category> lineage = new ArrayList<>();
        Category current = selectedCategory;
        while (current != null) {
            lineage.add(current);
            current = current.getParentId() != null ? allCategoriesMap.get(current.getParentId()) : null;
        }
        java.util.Collections.reverse(lineage);

        List<String> added = new ArrayList<>();
        boolean hasFields = false;

        for (Category cat : lineage) {
            if (cat.getRequiredFields() == null || cat.getRequiredFields().isEmpty()) continue;
            List<String> toAdd = new ArrayList<>();
            for (String f : cat.getRequiredFields()) if (!added.contains(f)) { toAdd.add(f); added.add(f); }
            if (toAdd.isEmpty()) continue;

            hasFields = true;
            Label header = new Label(cat.getName() + " Specifications");
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-padding: 10 0 5 0;");
            dynamicFieldsContainer.getChildren().add(header);

            for (String fieldName : toAdd) {
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label lbl = new Label(fieldName + ":"); lbl.setPrefWidth(140);
                TextField input = new TextField(); input.setPromptText("Enter " + fieldName.toLowerCase() + "...");
                HBox.setHgrow(input, javafx.scene.layout.Priority.ALWAYS);
                row.getChildren().addAll(lbl, input);
                dynamicFieldsContainer.getChildren().add(row);
                dynamicTextFieldsMap.put(cat.getId() + ":" + fieldName, input);
            }
        }

        if (!hasFields) {
            Label none = new Label("No specific specifications required.");
            none.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
            dynamicFieldsContainer.getChildren().add(none);
        }
    }

    // --- Image pickers ---
    @FXML private void handleChooseMainImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Main Image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(titleField.getScene().getWindow());
        if (f != null) { mainImageFile = f; mainImageNameLabel.setText(f.getName()); }
    }

    @FXML private void handleChooseSubImages() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Gallery Images");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        List<File> files = fc.showOpenMultipleDialog(titleField.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            subImageFiles.clear(); subImageFiles.addAll(files);
            subImagesCountLabel.setText(files.size() + " images selected");
        }
    }

    // --- Cancel ---
    @FXML private void handleCancel() {
        // Reset form
        titleField.clear(); descriptionArea.clear(); startingPriceField.clear();
        mainImageFile = null; subImageFiles.clear();
        mainImageNameLabel.setText("No file selected");
        subImagesCountLabel.setText("0 images selected");
        categoryComboBox.getSelectionModel().clearSelection();
        dynamicFieldsContainer.getChildren().clear();
        statusLabel.setText("");
    }

    // --- Submit ---
    @FXML
    private void handleSubmitAuction() {
        statusLabel.setTextFill(javafx.scene.paint.Color.RED);
        statusLabel.setText("");

        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        Category selectedCategory = categoryComboBox.getValue();
        String priceText = startingPriceField.getText().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory == null || priceText.isEmpty()) {
            statusLabel.setText("Vui lòng điền đầy đủ thông tin cơ bản.");
            return;
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(priceText);
            if (startingPrice <= 0) { statusLabel.setText("Giá khởi điểm phải lớn hơn 0."); return; }
        } catch (NumberFormatException e) {
            statusLabel.setText("Giá khởi điểm không hợp lệ."); return;
        }

        // Xử lý ảnh
        byte[] mainImageBytes = null;
        List<byte[]> subImageBytesList = new ArrayList<>();
        try {
            if (mainImageFile != null) mainImageBytes = ImageOptimizer.optimizeImage(mainImageFile);
            for (File f : subImageFiles) subImageBytesList.add(ImageOptimizer.optimizeImage(f));
        } catch (IOException e) {
            statusLabel.setText("Lỗi xử lý ảnh!"); return;
        }

        // Dynamic specs
        Map<Integer, Map<String, String>> categoryGroupedSpecs = new HashMap<>();
        for (Map.Entry<String, TextField> entry : dynamicTextFieldsMap.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int catId = Integer.parseInt(parts[0]);
            String specName = parts[1];
            String val = entry.getValue().getText().trim();
            if (val.isEmpty()) { statusLabel.setText("Thiếu thông tin: " + specName); return; }
            categoryGroupedSpecs.computeIfAbsent(catId, k -> new HashMap<>()).put(specName, val);
        }

        boolean isDurationMode = durationRadio.isSelected();
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        long duration = 0;
        AuctionStatus initialStatus;

        if (isDurationMode) {
            // --- Chế độ Duration: WAITING, chờ user bấm Start Now trong My Auctions ---
            try {
                daysSpinner.commitValue();
                hoursSpinner.commitValue();
                minsSpinner.commitValue();
            } catch (Exception e) {
                statusLabel.setText("Định dạng thời lượng không hợp lệ!"); return;
            }
            int days = daysSpinner.getValue();
            int hours = hoursSpinner.getValue();
            int mins = minsSpinner.getValue();

            if (days == 0 && hours == 0 && mins == 0) {
                statusLabel.setText("Thời lượng phải lớn hơn 0."); return;
            }

            duration = (days * 86400L) + (hours * 3600L) + (mins * 60L);
            startTime = null;
            endTime = null;
            initialStatus = AuctionStatus.WAITING;
            ClientLogger.info("Creating WAITING auction with duration " + duration + "s");

        } else {
            // --- Chế độ Schedule: SCHEDULED, server tự kích hoạt tại startTime ---
            if (startDatePicker.getValue() == null) {
                statusLabel.setText("Vui lòng chọn ngày bắt đầu."); return;
            }
            if (endDatePicker.getValue() == null) {
                statusLabel.setText("Vui lòng chọn ngày kết thúc."); return;
            }

            int sH = startHourCombo.getValue() != null ? startHourCombo.getValue() : 0;
            int sM = startMinCombo.getValue() != null ? startMinCombo.getValue() : 0;
            startTime = LocalDateTime.of(startDatePicker.getValue(), LocalTime.of(sH, sM));

            int eH = endHourCombo.getValue() != null ? endHourCombo.getValue() : 0;
            int eM = endMinCombo.getValue() != null ? endMinCombo.getValue() : 0;
            endTime = LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(eH, eM));

            if (!startTime.isAfter(LocalDateTime.now())) {
                statusLabel.setText("Thời gian bắt đầu phải ở tương lai."); return;
            }
            if (!endTime.isAfter(startTime)) {
                statusLabel.setText("Thời gian kết thúc phải sau thời gian bắt đầu."); return;
            }

            duration = 0;
            initialStatus = AuctionStatus.SCHEDULED;
            ClientLogger.info("Creating SCHEDULED auction: " + startTime + " → " + endTime);
        }

        // 3. Packages and submits to Server pipeline
        int categoryId = selectedCategory.getId();

        ClientLogger.info("Form validation successful. Transmitting new auction layout details...");

        CreateAuctionRequest request = new CreateAuctionRequest(title, description, selectedCategory, categoryGroupedSpecs, 
                                                                mainImageBytes, subImageBytesList, startingPrice, 
                                                                duration, startTime, endTime, initialStatus);
        RequestSender.send(request);
        ClientLogger.info("CreateAuctionRequest sent.");
    }

    private void handleCreateAuctionResponse(CreateAuctionResponse response) {
        Platform.runLater(() -> {
            submitButton.setDisable(false);
            submitButton.setText("Tạo phiên đấu giá");

            if (response.isSuccess()) {
                statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                boolean isWaiting = response.getAuction() != null
                        && response.getAuction().getStatus() == AuctionStatus.WAITING;
                if (isWaiting) {
                    statusLabel.setText("✅ Đã tạo thành công! Vào 'My Auctions' để bấm 'Bắt đầu ngay' khi sẵn sàng.");
                } else {
                    statusLabel.setText("✅ Phiên đấu giá đã được lên lịch thành công!");
                }
                // Reset form sau khi tạo thành công
                handleCancel();
            } else {
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText("❌ " + response.getMessage());
            }
        });
    }
}
