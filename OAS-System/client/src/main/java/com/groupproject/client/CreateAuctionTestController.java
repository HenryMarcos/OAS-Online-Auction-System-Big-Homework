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
import com.groupproject.client.utils.TimeUtil;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.responses.CreateAuctionResponse;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
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
    // Các thuộc tính cơ bản
    // ---------------------
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private Label mainImageNameLabel;
    @FXML private Label subImagesCountLabel;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField startingPriceField;
    @FXML private VBox dynamicFieldsContainer;
    @FXML private Label statusLabel;

    // Nút start now để cho phiên đấu giá tạo ra bắt đầu luôn
    @FXML private CheckBox startNowCheckBox;

    // Lựa chọn đổi giữa chọn thời gian và chọn ngày
    @FXML private ToggleGroup timingToggleGroup;
    @FXML private RadioButton durationRadio;
    @FXML private RadioButton dateRadio;

    // Chế độ chọn thời lượng buổi đấu giá
    @FXML private Label durationLabel;
    @FXML private HBox durationInputBox;
    @FXML private Spinner<Integer> daysSpinner;
    @FXML private Spinner<Integer> hoursSpinner;
    @FXML private Spinner<Integer> minsSpinner;

    // Chế độ chọn theo ngày
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
        // Nối response với hàm tương ứng
        ClientMessageRouter.INSTANCE.onResponse(CreateAuctionResponse.class, this::handleCreateAuctionResponse);

        // Setup cách categories hiển thị trong ComboBox
        setupCategoryComboBoxFormatting();

        // Lấy categories
        List<Category> mainCategories = SessionManager.INSTANCE.getCurrentCategories();
        if (mainCategories != null) {
            populateCategoryData(mainCategories);
        }

        // Kiểm tra sự thay đổi trong lựa chọn category và thay vào các field phù hợp
        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                generateDynamicFields(newValue);
            }
        });

        setupSpinnersAndCombos();
        setupTimingVisibilityListeners();
        updateTimingFieldsLayout(); // Initial trigger layout execution
    }

    private void setupTimingVisibilityListeners() {
        // Trigger rearrangement when switching Radio Button options
        timingToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateTimingFieldsLayout());
        
        // Trigger structural rearrangement when checking/unchecking "Start Now"
        startNowCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateTimingFieldsLayout());
    }

    private void setupSpinnersAndCombos() {
        // Initialize Duration Spinners
        daysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 365, 0));
        hoursSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 1));
        minsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // Populate Time Dropdowns
        for (int i = 0; i < 24; i++) {
            startHourCombo.getItems().add(i);
            endHourCombo.getItems().add(i);
        }
        for (int i = 0; i < 60; i += 5) { // Intervals of 5 mins
            startMinCombo.getItems().add(i);
            endMinCombo.getItems().add(i);
        }

        // Default Dropdown Pick selections
        startHourCombo.getSelectionModel().select(12);
        startMinCombo.getSelectionModel().select(0);
        endHourCombo.getSelectionModel().select(12);
        endMinCombo.getSelectionModel().select(0);
    }

    private void updateTimingFieldsLayout() {
        boolean isDurationMode = durationRadio.isSelected();
        boolean isStartNow = startNowCheckBox.isSelected();

        // 1. Handle Duration Mode Row Items Visibility
        durationLabel.setVisible(isDurationMode);
        durationLabel.setManaged(isDurationMode);
        durationInputBox.setVisible(isDurationMode);
        durationInputBox.setManaged(isDurationMode);

        // 2. Handle Date Mode Row Items Visibility
        startDateLabel.setVisible(!isDurationMode);
        startDateLabel.setManaged(!isDurationMode);
        startDateBox.setVisible(!isDurationMode);
        startDateBox.setManaged(!isDurationMode);

        endDateLabel.setVisible(!isDurationMode);
        endDateLabel.setManaged(!isDurationMode);
        endDateBox.setVisible(!isDurationMode);
        endDateBox.setManaged(!isDurationMode);

        // 3. Conditional state rules for 'Start Now' selection
        if (!isDurationMode) {
            // If starting instantly, manual calculation of target start picker is locked out
            startDatePicker.setDisable(isStartNow);
            startHourCombo.setDisable(isStartNow);
            startMinCombo.setDisable(isStartNow);
            if (isStartNow) {
                startDatePicker.setValue(LocalDateTime.now().toLocalDate());
                startHourCombo.getSelectionModel().select(Integer.valueOf(LocalDateTime.now().getHour()));
                startMinCombo.getSelectionModel().select(Integer.valueOf(LocalDateTime.now().getMinute() - (LocalDateTime.now().getMinute() % 5)));
            }
        }
    }


    // Thêm các category vào cho người dùng lựa chọn
    private void populateCategoryData(List<Category> mainCategories) {
        allCategoriesMap.clear();
        categoryComboBox.getItems().clear();

        for (Category mainCat : mainCategories) {
            addCategoryToSelection(mainCat, 0);
        }
    }

    private void addCategoryToSelection(Category cat, int depth) {
        allCategoriesMap.put(cat.getId(), cat);

        // Lưu tên danh mục
        categoryComboBox.getItems().add(cat);

        // Tìm các category con(nếu có)
        if (cat.getSubCategories() != null) {
            for (Category subCat : cat.getSubCategories()) {
                addCategoryToSelection(subCat, depth + 1);
            }
        }
    }

    // Thay đổi hình thức hiển thị của các mục bên trong ComboBox để các danh mục con trông thụt vào trong.
    private void setupCategoryComboBoxFormatting() {
        ClientLogger.info("Setting up category ComboxBox formatting");
        categoryComboBox.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                if (category == null) return "";

                // Nếu category có parent, thì thêm dấu mũi tên
                StringBuilder prefix = new StringBuilder();
                Category temp = category;
                while (temp.getParentId() != null && allCategoriesMap.containsKey(temp.getParentId())) {
                    prefix.insert(0, "  -> ");
                    temp = allCategoriesMap.get(temp.getParentId());
                }
                return prefix.toString() + category.getName();
            }

            @Override
            public Category fromString(String string) { return null; }
        });
        ClientLogger.info("Finish setting up category ComboxBox formatting");
    }

    // Lấy các field mà category hiện tại có cùng các field cần thiết của category cha
    // -------------------------------------------------------------------------------
    private void generateDynamicFields(Category selectedCategory) {
        ClientLogger.info("Generating dynamic fields for Scroll UI");
        
        // 1. Dọn sạch các field cũ đi
        // ---------------------------
        dynamicFieldsContainer.getChildren().clear();
        dynamicTextFieldsMap.clear();

        if (selectedCategory == null) return; // Nếu mà category ko tồn tại thì trả về null

        // 2. Lấy các category từ child đến parent
        // ---------------------------------------
        List<Category> categoryLineage = new ArrayList<>();
        Category current = selectedCategory;

        while (current != null) {
            categoryLineage.add(current);
            if (current.getParentId() != null) {
                current = allCategoriesMap.get(current.getParentId());
            } else {
                current = null;
            }
        }

        // 3. Đảo ngược lại để in các field của parent trước rồi mới đến child
        // -------------------------------------------------------------------
        java.util.Collections.reverse(categoryLineage);

        boolean hasAnyFields = false;
        List<String> alreadyAddedFields = new ArrayList<>();

        // 4. Build the UI rows
        for (Category cat : categoryLineage) {
            
            if (cat.getRequiredFields() != null && !cat.getRequiredFields().isEmpty()) {
                
                List<String> fieldsToAdd = new ArrayList<>();
                for (String field : cat.getRequiredFields()) {
                    if (!alreadyAddedFields.contains(field)) {
                        fieldsToAdd.add(field);
                        alreadyAddedFields.add(field);
                    }
                }

                if (!fieldsToAdd.isEmpty()) {
                    hasAnyFields = true;

                    // --- ADD CATEGORY HEADER LABEL ---
                    Label categoryHeader = new Label(cat.getName() + " Specifications");
                    categoryHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-padding: 10 0 5 0;");
                    dynamicFieldsContainer.getChildren().add(categoryHeader);

                    // --- ADD TEXT FIELDS ---
                    for (String fieldName : fieldsToAdd) {
                        HBox fieldRow = new HBox(10);
                        fieldRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        Label label = new Label(fieldName + ":");
                        label.setPrefWidth(140);
                        
                        TextField inputField = new TextField();
                        inputField.setPromptText("Enter " + fieldName.toLowerCase() + "...");
                        HBox.setHgrow(inputField, javafx.scene.layout.Priority.ALWAYS);

                        fieldRow.getChildren().addAll(label, inputField);
                        dynamicFieldsContainer.getChildren().add(fieldRow);

                        // Save textfield for submit data extraction
                        dynamicTextFieldsMap.put(cat.getId() + ":" + fieldName, inputField);
                    }
                }
            }
        }

        // 5. If no fields exist for this category tree
        if (!hasAnyFields) {
            Label noFieldsLabel = new Label("No specific specifications required.");
            noFieldsLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
            dynamicFieldsContainer.getChildren().add(noFieldsLabel);
        }

        ClientLogger.info("Finished generating dynamic fields");
    }

    @FXML
    private void handleChooseMainImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Main Image");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        // Open the dialog. We use titleField.getScene().getWindow() to tie the dialog to the current window
        File selectedFile = fileChooser.showOpenDialog(titleField.getScene().getWindow());
        
        if (selectedFile != null) {
            mainImageFile = selectedFile;
            mainImageNameLabel.setText(selectedFile.getName());
        }
    }

    @FXML
    private void handleChooseSubImages() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Gallery Images");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        // Note: showOpenMultipleDialog allows selecting multiple files at once
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(titleField.getScene().getWindow());
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            subImageFiles.clear();
            subImageFiles.addAll(selectedFiles);
            subImagesCountLabel.setText(subImageFiles.size() + " images selected");
        }
    }
    
    @FXML
    private void handleSubmitAuction() {
        ClientLogger.info("Handling submit auction");
        statusLabel.setText("");

        // Lấy các input
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        Category selectedCategory = categoryComboBox.getValue();
        String priceText = startingPriceField.getText().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory == null || priceText.isEmpty()) {
            statusLabel.setText("Please fill out all basic auction fields.");
            return;
        }

        double startingPrice;

        try {
            startingPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Starting price must be a valid number.");
            return;
        }

        // Xử lý hình ảnh
        byte[] mainImageBytes = null;
        List<byte[]> subImageBytesList = new ArrayList<>();

        try {
            // Compress Main Image
            if (mainImageFile != null) {
                mainImageBytes = ImageOptimizer.optimizeImage(mainImageFile); 
            }
            // Compress Sub Images
            for (File file : subImageFiles) {
                subImageBytesList.add(ImageOptimizer.optimizeImage(file));
            }
        } catch (IOException e) {
            statusLabel.setText("Error processing image files!");
            return;
        }

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        AuctionStatus initialStatus;

        // Lưu các field của từng category theo category id
        Map<Integer, Map<String, String>> categoryGroupedSpecs = new HashMap<>();

        // 2. COLLECT DATA FROM DYNAMIC FIELDS!
        // entry lưu trữ theo format "categoryId:fieldName" -> TextField
        for (Map.Entry<String, TextField> entry : dynamicTextFieldsMap.entrySet()) {
            String compoundKey = entry.getKey();
            String specValue = entry.getValue().getText().trim();

            // Chia compound key thành id và field name
            String[] parts = compoundKey.split(":");
            int fieldCategoryId = Integer.parseInt(parts[0]);
            String specName = parts[1];

            if (specValue.isEmpty()) {
                statusLabel.setText("Missing spec field: " + specName);
                return; // Stop processing if form incomplete
            }
            
            // Thêm vào gồm id của category, tên của field và input của field
            categoryGroupedSpecs.computeIfAbsent(fieldCategoryId, k -> new HashMap<>()).put(specName, specValue);
        }

        boolean isStartNow = startNowCheckBox.isSelected();
        boolean isDurationMode = durationRadio.isSelected();

        long duration = 0;

        if (isStartNow) {
            startTime = TimeUtil.getNow();
            initialStatus = AuctionStatus.ACTIVATED;

            if (isDurationMode) {
                try {
                    daysSpinner.commitValue();
                    hoursSpinner.commitValue();
                    minsSpinner.commitValue();
                } catch (Exception e) {
                    statusLabel.setText("Invalid duration format!");
                    return;
                }

                int days = daysSpinner.getValue();
                int hours = hoursSpinner.getValue();
                int mins = minsSpinner.getValue();

                ClientLogger.info(String.format("Auction have duration: %d days, %d hours, %d mins", days, hours, mins));

                if (days == 0 && hours == 0 && mins == 0) {
                    statusLabel.setText("Duration cannot be 0!");
                    return;
                }

                duration = (days * 86400L) + (hours * 3600L) + (mins * 60L);

                endTime = startTime.plusSeconds(duration);

                ClientLogger.info("Start date: " + startTime + ", End date: " + endTime);
            } else {
                // Explicit End Date Calculation Mode
                if (endDatePicker.getValue() == null) {
                    statusLabel.setText("Please select an End Date!");
                    return;
                }
                int hour = endHourCombo.getValue() != null ? endHourCombo.getValue() : 0;
                int min = endMinCombo.getValue() != null ? endMinCombo.getValue() : 0;
                endTime = LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(hour, min));

                if (endTime.isBefore(startTime)) {
                    statusLabel.setText("End time must be after right now!");
                    return;
                }
            }
        } else {
            // "Start Now" is false -> System stays in WAITING phase
            initialStatus = AuctionStatus.WAITING;

            if (isDurationMode) {
                int days = daysSpinner.getValue();
                int hours = hoursSpinner.getValue();
                int mins = minsSpinner.getValue();

                duration = (days * 86400L) + (hours * 3600L) + (mins * 60L);

                // Wait until manually activated on Server to run real calculation timestamps
                startTime = null;
                endTime = null;
            } else {
                // explicit date options provided for the future activation point
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                    statusLabel.setText("Please pick both Start and End Dates!");
                    return;
                }
                int sHour = startHourCombo.getValue() != null ? startHourCombo.getValue() : 0;
                int sMin = startMinCombo.getValue() != null ? startMinCombo.getValue() : 0;
                startTime = LocalDateTime.of(startDatePicker.getValue(), LocalTime.of(sHour, sMin));

                int eHour = endHourCombo.getValue() != null ? endHourCombo.getValue() : 0;
                int eMin = endMinCombo.getValue() != null ? endMinCombo.getValue() : 0;
                endTime = LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(eHour, eMin));

                if (endTime.isBefore(startTime)) {
                    statusLabel.setText("End Date cannot be scheduled before the Start Date!");
                    return;
                }
            }
        }

        // 3. Packages and submits to Server pipeline
        int categoryId = selectedCategory.getId();

        ClientLogger.info("Form validation successful. Transmitting new auction layout details...");

        CreateAuctionRequest request = new CreateAuctionRequest(title, description, selectedCategory, categoryGroupedSpecs, 
                                                                mainImageBytes, subImageBytesList, startingPrice, 
                                                                duration, startTime, endTime, initialStatus);
        RequestSender.send(request);

        ClientLogger.info("Finish handling submit auction");
    }

    private void handleCreateAuctionResponse(CreateAuctionResponse response) {
        if (response.isSuccess()) { handleSuccessfulCreateAuction(response); }
        else { handleFailedCreateAuction(response); }
    }

    private void handleSuccessfulCreateAuction(CreateAuctionResponse response) {
        ClientLogger.info("Successfully created new auction");
    }

    private void handleFailedCreateAuction(CreateAuctionResponse response) {
        ClientLogger.error("failed to create new auction");
    }
}


