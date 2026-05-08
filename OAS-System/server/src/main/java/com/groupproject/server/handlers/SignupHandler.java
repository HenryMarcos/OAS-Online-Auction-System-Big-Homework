package com.groupproject.server.handlers;

import java.io.ObjectOutputStream;
import java.util.List;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;
import com.groupproject.shared.network.SignupRequest;
import com.groupproject.shared.network.SignupResponse;

public class SignupHandler implements RequestHandler {
    public void handle(Request request, ObjectOutputStream out) throws Exception {
        boolean success;
        if (request instanceof SignupRequest) success = false;
        else success = UserDAO.saveUser((SignupRequest) request);

        out.writeObject(success ? "SERVER:AUTH_SUCCESS" : "SERVER:AUTH_FAIL:User exists!");
        out.flush();

        // Gửi danh sách các danh mục cho client
        if (success) {
            List<Category> categoryTree = CategoryDAO.getCategories();
            out.writeObject(categoryTree);
            out.flush();
        }
    }

    @Override
    public Response handle(Request request) {
        boolean success;
        if (!(request instanceof SignupRequest)) success = false;
        else success = UserDAO.saveUser((SignupRequest) request);

        if (success) { return new SignupResponse(true, null, CategoryDAO.getCategories(), "Welcome back!"); }
        else { return new SignupResponse(false, "Invalid username or password"); }
    }
}
