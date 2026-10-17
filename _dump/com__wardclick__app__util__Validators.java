package com.wardclick.app.util;

import android.util.Patterns;

public class Validators {
    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidPhone(String phone) {
        // Optional phone, if empty considered valid
        if (phone == null || phone.trim().isEmpty()) return true;
        // Basic check for digits and symbols
        return phone.matches("^\\+?[0-9\\-\\s()]+$");
    }
}
