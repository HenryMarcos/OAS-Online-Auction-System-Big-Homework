package com.groupproject.shared.network;

import java.util.List;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.user.User;

public class SignupResponse extends Response {
    private User user;
    private List<Category> categoryTree;

    public SignupResponse(boolean success, User user, List<Category> categoryTree, String message) {
        super(success, message);
        this.user = user;
        this.categoryTree = categoryTree;
    }

    public SignupResponse(boolean success, String message) {
        super(success, message);
    }

    @Override
    public String getType() { return "SIGNUP_RESULT"; }

    public User getUser() { return user; }
    public List<Category> getCategoryTree() { return categoryTree; }
}
