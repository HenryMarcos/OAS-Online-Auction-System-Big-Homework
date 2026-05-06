package com.groupproject.shared.model.categories;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private Integer parentId; // Dùng Integer để có thể đặt null
    private List<Category> subCategories;
    private List<String> requiredFields;

    public Category(int id, String name, Integer parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.subCategories = new ArrayList<>();
        this.requiredFields = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public Integer getParentId() { return parentId; }
    public List<Category> getSubCategories() { return subCategories; }
    public List<String> getRequiredFields() { return requiredFields; }

    public void addSubCategory(Category child) {
        this.subCategories.add(child);
    }

    public void addRequiredField(String fieldName) {
        this.requiredFields.add(fieldName);
    }


    @Override
    public String toString() {
        return name;
    }

    public void print(String tab) {
        System.out.println(tab + this);
        System.out.printf(tab + "Fields: ");
        for (String field : requiredFields) {
            System.out.printf(field + ", ");
        }
        System.out.printf("\n");
        for (Category category : subCategories) {
            category.print(tab + "\t");
        }
    }
}
