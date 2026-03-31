module com.groupproject.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Thông báo Java rằng cần mô-đun từ shared
    requires com.groupproject.shared;

    opens com.groupproject.client to javafx.fxml, javafx.graphics;
    exports com.groupproject.client;
}
