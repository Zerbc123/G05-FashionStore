package vn.edu.fpt.fashionstore.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Hash cần kiểm tra
        String hashedPassword = "$2a$10$7sF9uZ6C8WwQ7R1pZK9V0eQxF7p7JkK9kXQ1m9nB5yW6b3zYQpG8K";
        
        // Test các password
        String[] testPasswords = {
            "123456",
            "admin123", 
            "password",
            "123",
            "admin"
        };
        
        System.out.println("=== PASSWORD VERIFICATION ===");
        System.out.println("Hash: " + hashedPassword);
        System.out.println();
        
        for (String password : testPasswords) {
            boolean matches = encoder.matches(password, hashedPassword);
            System.out.println("Password: " + password + " -> " + 
                             (matches ? "✅ MATCH" : "❌ NO MATCH"));
        }
        
        // Tạo hash mới cho password 123456
        System.out.println("\n=== NEW HASH FOR '123456' ===");
        for (int i = 0; i < 3; i++) {
            String newHash = encoder.encode("123456");
            System.out.println("Hash " + (i+1) + ": " + newHash);
        }
    }
}
