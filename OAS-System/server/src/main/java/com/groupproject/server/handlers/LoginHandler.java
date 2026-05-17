package com.groupproject.server.handlers;

import java.io.ObjectOutputStream;
import java.util.List;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.network.LoginRequest;
import com.groupproject.shared.network.LoginResponse;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

// lam viec tren 
public class LoginHandler implements RequestHandler {
    public void handle(Request request, ObjectOutputStream out) throws Exception {
        boolean success;
        if (request instanceof LoginRequest) success = false;
        else success = UserDAO.checkUser((LoginRequest) request);

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
    
    // Tập trung vào phần này 
    @Override
    public Response handle(Request request) {
        boolean success;
        if (!(request instanceof LoginRequest)) success = false;
        else success = UserDAO.checkUser((LoginRequest) request);

        if (success) { return new LoginResponse(true, null, CategoryDAO.getCategories(), "Welcome back!"); }
        else { return new LoginResponse(false, "Invalid username or password"); }
    }
}
