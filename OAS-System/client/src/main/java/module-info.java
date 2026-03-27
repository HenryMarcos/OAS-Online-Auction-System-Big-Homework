module com.groupproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.groupproject to javafx.fxml, javafx.graphics;
    exports com.groupproject;
}
