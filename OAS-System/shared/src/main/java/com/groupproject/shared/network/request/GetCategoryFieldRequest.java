package com.groupproject.shared.network.request;

public class GetCategoryFieldRequest extends Request {
    private int categoryId;

    public GetCategoryFieldRequest(int categoryId) {
        this.categoryId = categoryId;
    } 

    public int getCategoryId() { return categoryId; }
}
