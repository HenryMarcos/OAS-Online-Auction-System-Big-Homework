package com.groupproject.shared.model.user;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private int adminLevel; // Cấp độ quản trị, tăng dần từ 1 tới 2, với 2 là cấp độ cao nhất có quyền kiểm soát toàn bộ hệ thống

    public Admin() {
        super();
        this.adminLevel = 1; // Mặc định cấp độ quản trị là 1
    }

    public Admin(int adminLevel, String username, String password, String email) {
        super(username, password, email);
        if (adminLevel < 1 || adminLevel > 2) {
            throw new IllegalArgumentException("Cấp độ quản trị phải là 1 hoặc 2.");
        }
        this.adminLevel = adminLevel;
    }

    public int getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(int adminLevel) {
        if (adminLevel < 1 || adminLevel > 2) {
            throw new IllegalArgumentException("Cấp độ quản trị phải là 1 hoặc 2.");
        }
        this.adminLevel = adminLevel;
    }

}
