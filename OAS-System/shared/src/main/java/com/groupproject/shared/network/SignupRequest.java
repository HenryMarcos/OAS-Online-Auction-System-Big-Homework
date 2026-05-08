package com.groupproject.shared.network;

public class SignupRequest extends Request {
    private String username;
    private String email;
    private String password;
    private String repeatPassword;

    public SignupRequest(String username, String email, String password, String repeatPassword) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.repeatPassword = repeatPassword;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRepeatedPassword() { return repeatPassword; }
}
