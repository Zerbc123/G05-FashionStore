package vn.edu.fpt.fashionstore.util;

import java.util.regex.Pattern;

public class PhoneUtils {
    // Vietnamese phone number: starts with 0 and 10 digits total
    private static final Pattern VN_PHONE = Pattern.compile("^0[2-9][0-9]{8}$");

    public static boolean isValid(String phone) {
        if (phone == null) return false;
        String cleaned = phone.replaceAll("[^0-9]", "");
        return VN_PHONE.matcher(cleaned).matches();
    }
}
