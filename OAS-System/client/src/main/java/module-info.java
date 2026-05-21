module com.groupproject.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.slf4j;
    // Thông báo Java rằng cần mô-đun từ shared
    requires com.groupproject.shared;
    requires javafx.base;
    opens com.groupproject.client to javafx.fxml, javafx.graphics;
    exports com.groupproject.client;
    exports com.groupproject.client.network;
}
