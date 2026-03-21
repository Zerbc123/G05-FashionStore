package vn.edu.fpt.fashionstore.util;

import java.util.regex.Pattern;

/**
 * Utility class for password validation rules used across the application.
 */
public class PasswordUtils {
    // 8-12 characters, letters (uppercase/lowercase), digits, and special characters (no accents/diacritics)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?]{8,12}$");

    /**
     * Checks whether the provided password matches the application's policy.
     *
     * Policy:
     * <ul>
     *   <li>8-12 characters long</li>
     *   <li>Contains at least one uppercase letter (A-Z)</li>
     *   <li>Contains at least one lowercase letter (a-z)</li>
     *   <li>Contains at least one digit (0-9)</li>
     *   <li>May contain special characters like !@#$%^&*()_+-=[]{};':",./<>?</li>
     *   <li>No accent marks or diacritics</li>
     * </ul>
     *
     * @param password the password to validate
     * @return true if it meets the policy, false otherwise
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        
        // Check length first
        if (password.length() < 8 || password.length() > 12) {
            return false;
        }
        
        // Check if it matches the allowed character pattern
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return false;
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }
        
        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        
        return true;
    }
}
