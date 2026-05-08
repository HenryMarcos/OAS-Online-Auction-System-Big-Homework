package com.groupproject.shared.network;

import java.util.List;

import com.groupproject.shared.model.categories.Category;

public class GetCategoriesResponse extends Response {
    public List<Category> categories;

    public GetCategoriesResponse(boolean success, List<Category> categories) {
        super(success);
        this.categories = categories;
    }

    @Override
    public String getType() { return "GET_CATEGORIES_RESULT"; }
}
