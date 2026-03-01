package vn.edu.fpt.fashionstore.util;

public class AddressUtils {
    /**
     * Cải thiện validation: Chỉ cần địa chỉ không rỗng và có độ dài tối thiểu
     * Hỗ trợ nhiều định dạng khác nhau
     */
    public static boolean isValid(String address) {
        if (address == null) return false;
        
        String trimmed = address.trim();
        if (trimmed.isEmpty()) return false;
        
        // Độ dài tối thiểu 10 ký tự (ví dụ: "A, B, C, D")
        if (trimmed.length() < 10) return false;
        
        // Kiểm tra có chứa ký tự hợp lệ không
        if (!trimmed.matches("^[\\p{L}\\p{N}\\s\\d\\,\\-\\.]+$")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Method mới: Kiểm tra địa chỉ có đủ thành phần cơ bản
     */
    public static boolean hasBasicComponents(String address) {
        if (address == null) return false;
        
        String trimmed = address.trim();
        if (trimmed.isEmpty()) return false;
        
        // Kiểm tra có chứa từ khóa địa chỉ Việt Nam không
        String[] vietnamKeywords = {"quận", "huyện", "phường", "xã", "tp", "thành phố", "tỉnh"};
        String lowerAddress = trimmed.toLowerCase();
        
        for (String keyword : vietnamKeywords) {
            if (lowerAddress.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}
