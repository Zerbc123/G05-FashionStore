package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @GetMapping("/test-password")
    public String testPassword(@RequestParam(defaultValue = "123456") String password) {
        String hashedPassword = "$2a$10$7sF9uZ6C8WwQ7R1pZK9V0eQxF7p7JkK9kXQ1m9nB5yW6b3zYQpG8K";
        
        boolean matches = passwordEncoder.matches(password, hashedPassword);
        
        return "Hash: " + hashedPassword + "<br>" +
               "Test Password: " + password + "<br>" +
               "Matches: " + (matches ? "✅ TRUE" : "❌ FALSE") + "<br><br>" +
               "<a href='/test-password?password=admin123'>Test admin123</a><br>" +
               "<a href='/test-password?password=123'>Test 123</a>";
    }
    
    @GetMapping("/hash-password")
    public String hashPassword(@RequestParam String password) {
        String newHash = passwordEncoder.encode(password);
        return "Password: " + password + "<br>" +
               "New Hash: " + newHash;
    }
}
