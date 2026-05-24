module shared {
    // Bạn phải export từng folder con chứa Class
    exports com.groupproject.shared.model.user;
    exports com.groupproject.shared.model.base;
    exports com.groupproject.shared.network.request;
    exports com.groupproject.shared.network.response;
    exports com.groupproject.shared.network.event;
    // Nếu có thêm folder con nào khác (ví dụ: .utils, .categories), bạn cũng phải thêm vào đây
    
    // Cần thiết để Java Serialization có thể làm việc qua module
    opens com.groupproject.shared.model.user to server, client;
    opens com.groupproject.shared.network.request to server, client;
}