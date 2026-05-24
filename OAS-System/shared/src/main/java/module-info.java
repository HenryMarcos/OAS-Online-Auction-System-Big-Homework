module com.groupproject.shared { // Đổi tên ở đây cho chuẩn
    // Export các package để Server/Client có thể dùng
    exports com.groupproject.shared.model.categories;
    exports com.groupproject.shared.model.user;
    exports com.groupproject.shared.model.base;
    exports com.groupproject.shared.network.event;
    exports com.groupproject.shared.network.request;
    exports com.groupproject.shared.network.response;
    exports com.groupproject.shared.model.enums;
    exports com.groupproject.shared.model.transaction;

    // Cho phép Java Serialization làm việc (Quan trọng cho Socket)
    opens com.groupproject.shared.model.user;
    opens com.groupproject.shared.model.base;
    opens com.groupproject.shared.model.categories;
    opens com.groupproject.shared.network.event;
    opens com.groupproject.shared.network.request;
    opens com.groupproject.shared.network.response;
}