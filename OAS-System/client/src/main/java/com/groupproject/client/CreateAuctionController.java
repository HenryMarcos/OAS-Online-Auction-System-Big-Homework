package com.groupproject.client;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.groupproject.client.network.EventRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.network.CreateAuctionRequest;
import com.groupproject.shared.network.CreateAuctionResponse;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class CreateAuctionController implements Initializable {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField startingPriceField;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Integer> hoursComboBox;
    @FXML private ComboBox<Integer> minutesComboBox;
    @FXML private ComboBox<Integer> secondsComboBox;
    @FXML private VBox dynamicFieldsContainer;
    @FXML private Label statusLabel;

    private final Map<Integer, Category> allCategoriesMap = new HashMap<>();

    private final Map<String, TextField> dynamicTextFieldsMap = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Nối response với hàm tương ứng
        EventRouter.getInstance().on(CreateAuctionResponse.class, this::handleCreateAuctionResponse);

        // Thêm lựa chọn giờ(từ 0 đến 23, 24 tính là 1 ngày nên không cần thêm)
        for (int i = 0; i < 24; i++) hoursComboBox.getItems().add(i);
        hoursComboBox.setValue(12); // Tạm đặt mặc định là 12h
        for (int i=0 ; i< 60 ; i++) {
            minutesComboBox.getItems().add(i);
        }
        minutesComboBox.setValue(0);
        for (int i=0 ; i<60; i++) {
             secondsComboBox.getItems().add(i);
        }
        secondsComboBox.setValue(0);
        // Setup cách categories hiển thị trong ComboBox
        setupCategoryComboBoxFormatting();

        // Lấy categories
        List<Category> mainCategories = SessionManager.getInstance().getCurrentCategories();
        if (mainCategories != null) {
            populateCategoryData(mainCategories);
        }

        // Kiểm tra sự thay đổi trong lựa chọn category và thay vào các field phù hợp
        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                generateDynamicFields(newValue);
            }
        });
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

    private void generateDynamicFields(Category selectedCategory) {
        ClientLogger.info("Generating dynamic fields for Scroll UI");
        
        // 1. Clear old fields
        dynamicFieldsContainer.getChildren().clear();
        dynamicTextFieldsMap.clear();

        if (selectedCategory == null) return;

        // 2. Get lineage (Child up to Root Parent)
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

        // 3. Reverse to print Parent first, then Child
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
    private void handleSubmitAuction() {
        ClientLogger.info("Handling submit auction");
        statusLabel.setText("");

        // Lấy các input
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        Category selectedCategory = categoryComboBox.getValue();
        String priceText = startingPriceField.getText().trim();
        java.time.LocalDate date = endDatePicker.getValue();

        if (title.isEmpty() || description.isEmpty() || selectedCategory == null || priceText.isEmpty() || date == null) {
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

        // Compute local timestamp
        int hours = hoursComboBox.getValue();
        int minutes = minutesComboBox.getValue();
        int seconds= secondsComboBox.getValue();
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.of(hours, minutes, seconds));

        // Lưu các field của từng category theo category id
        Map<Integer, Map<String, String>> categoryGroupedSpecs = new HashMap<>();

        // 2. COLLECT DATA FROM DYNAMIC FIELDS!
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

        // 3. Packages and submits to Server pipeline
        int currentUserId = SessionManager.getInstance().getCurrentUser().getId();
        int categoryId = selectedCategory.getId();

        ClientLogger.info("Form validation successful. Transmitting new auction layout details...");

        // TODO: Assemble your AuctionRequest payload object and forward it:
        // AuctionRequest req = new AuctionRequest(currentUserId, title, description, categoryId, startingPrice, endTime, capturedSpecifications);
        // RequestSender.send(req);

        CreateAuctionRequest request = new CreateAuctionRequest(currentUserId, title, description, selectedCategory, categoryGroupedSpecs, startingPrice, endTime.toString());
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