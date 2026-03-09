package vn.edu.fpt.fashionstore.util;

import java.util.regex.Pattern;

/**
 * Utility class for password validation rules used across the application.
 */
public class PasswordUtils {
    // exactly 6 characters, only ASCII letters and digits
    private static final Pattern SIX_ALPHANUMERIC = Pattern.compile("^[A-Za-z0-9]{6}$");

    /**
     * Checks whether the provided password matches the application's policy.
     *
     * Policy:
     * <ul>
     *   <li>Exactly 6 characters long</li>
     *   <li>Contains only letters (A–Z, a–z) and digits (0–9)</li>
     *   <li>No special characters or accent marks</li>
     * </ul>
     *
     * @param password the password to validate
     * @return true if it meets the policy, false otherwise
     */
    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return SIX_ALPHANUMERIC.matcher(password).matches();
    }
}
