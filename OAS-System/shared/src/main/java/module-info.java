module com.groupproject.shared {
    // 1. Export các nhóm Entity
    exports com.groupproject.shared;
    exports com.groupproject.shared.model.base;
    exports com.groupproject.shared.model.user;
    exports com.groupproject.shared.model.item;
    exports com.groupproject.shared.model.transaction;
    exports com.groupproject.shared.model.enums;

    // 2. Export các nhóm Factory
    exports com.groupproject.shared.factory;

    // 3. Export các nhóm Exception
    exports com.groupproject.shared.exception;
}
