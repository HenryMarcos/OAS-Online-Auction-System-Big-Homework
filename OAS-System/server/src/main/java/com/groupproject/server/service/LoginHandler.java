package com.groupproject.server.service;

import java.io.ObjectOutputStream;
import java.util.List;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.shared.AuthRequest;
import com.groupproject.shared.model.categories.Category;

public class LoginHandler implements AuthHandler {
    @Override
    public void handle(AuthRequest request, ObjectOutputStream out) throws Exception {
        boolean success = UserDAO.checkUser(request);

        // Gửi thông báo thành công cho client
        out.writeObject(success ? "SERVER:AUTH_SUCCESS" : "SERVER:AUTH_FAIL:Wrong credentials!");
        out.flush();

        // Gửi danh sách các danh mục cho client
        if (success) {
            List<Category> categoryTree = CategoryDAO.getCategories();
            out.writeObject(categoryTree);
            out.flush();
        }
    }
}
