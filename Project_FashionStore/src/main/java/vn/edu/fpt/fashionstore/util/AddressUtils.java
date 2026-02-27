package vn.edu.fpt.fashionstore.util;

public class AddressUtils {
    /**
     * Naive validation: must consist of at least 4 comma-separated segments
     * (house/street, ward, district, province) and none empty after trimming.
     */
    public static boolean isValid(String address) {
        if (address == null) return false;
        String[] parts = address.split(",");
        if (parts.length < 4) return false;
        for (String p : parts) {
            if (p.trim().isEmpty()) return false;
        }
        return true;
    }
}
