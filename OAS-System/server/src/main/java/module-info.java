open module com.groupproject.server {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    
    // Thông báo Java rằng cần mô-đun từ shared
    requires com.groupproject.shared;
    
    // Cấp quyền truy cập vào thư viện Database
    requires java.sql;

    requires com.zaxxer.hikari;
    requires org.slf4j;

    // 🌟 ĐÃ XÓA dòng opens cũ vì từ khóa 'open module' ở dòng 1 đã lo hết việc này rồi.
    
    exports com.groupproject.server;
}