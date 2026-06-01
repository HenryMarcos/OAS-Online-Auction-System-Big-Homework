module com.groupproject.shared { // Đổi tên ở đây cho chuẩn
    // Export các package để Server/Client có thể dùng
    exports com.groupproject.shared.model.categories;
    exports com.groupproject.shared.model.transaction;
    exports com.groupproject.shared.model.user;
    exports com.groupproject.shared.model.base;
    exports com.groupproject.shared.model.enums;
    //exports com.groupproject.shared.network;
    exports com.groupproject.shared.network.requests;
    exports com.groupproject.shared.network.responses;
    exports com.groupproject.shared.network.events;
    exports com.groupproject.shared.network.AuctionEvent;
    exports com.groupproject.shared.network;

    // Cho phép Java Serialization làm việc (Quan trọng cho Socket)
    opens com.groupproject.shared.model.user;
    opens com.groupproject.shared.model.base;
    opens com.groupproject.shared.model.categories;
    
}