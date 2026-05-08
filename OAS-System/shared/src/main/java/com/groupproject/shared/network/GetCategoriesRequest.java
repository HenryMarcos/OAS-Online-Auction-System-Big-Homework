package com.groupproject.shared.network;

import com.groupproject.shared.model.user.User;

public class GetCategoriesRequest extends Request {
    private User user;

    public GetCategoriesRequest(User user) {
        this.user = user;
    }
}
