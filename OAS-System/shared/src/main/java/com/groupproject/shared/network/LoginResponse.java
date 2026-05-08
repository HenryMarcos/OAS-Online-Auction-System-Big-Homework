package com.groupproject.shared.network;

import java.util.List;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.user.User;

public class LoginResponse extends Response {
    private User user;
    //List<Category> categoryTree = CategoryDAO.getCategories();
    private List<Category> categoryTree;

    public LoginResponse(boolean success, User user, List<Category> categoryTree, String message) {
        super(success, message);
        this.user = user;
        this.categoryTree = categoryTree;
    }

    public LoginResponse(boolean success, String message) {
        super(success, message);
    }

    @Override
    public String getType() { return "LOGIN_RESULT"; }

    public User getUser() { return user; }
    public List<Category> getCategoryTree() { return categoryTree; }
}
