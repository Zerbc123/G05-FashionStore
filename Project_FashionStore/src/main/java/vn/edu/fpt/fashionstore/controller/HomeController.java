package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "page"; // Trả về page.html
    }

    @GetMapping("/home")
    public String homePage() {
        return "page"; // Trả về page.html
    }

    @GetMapping(value = "/login")
    public String loginPage(){
        return "login";
    }

    @GetMapping(value = "/register")
    public String registerPage(){
        return "register";
    }

    @GetMapping(value = "/cart")
    public String cartPage(){
        return "cart"; // Trả về cart.html
    }

    @GetMapping(value = "/product-details")
    public String productDetailsPage(){
        return "productdetails"; // Trả về productdetails.html
    }

    @GetMapping(value = "/profile")
    public String profilePage(HttpSession session){
        // Kiểm tra nếu chưa login thì redirect về login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "profile"; // Trả về profile.html
    }

    @GetMapping(value = "/edit-profile")
    public String editProfilePage(HttpSession session){
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "editprofile"; // Trả về editprofile.html
    }

    @GetMapping(value = "/order-history")
    public String orderHistoryPage(HttpSession session){
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "viewhistory"; // Trả về viewhistory.html
    }

    @GetMapping(value = "/wishlist")
    public String wishlistPage(HttpSession session){
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "wishlist"; // Trả về wishlist.html
    }

    @GetMapping(value = "/order")
    public String orderPage(HttpSession session){
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "order"; // Trả về order.html
    }

    @GetMapping(value = "/orderdetails")
    public String orderDetailsPage(HttpSession session){
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "orderdetails"; // Trả về orderdetails.html
    }


    // Xử lý login - Hardcode credentials (cho testing layout)
    @PostMapping(value = "/login")
    public String handleLogin(@RequestParam String username, 
                             @RequestParam String password,
                             HttpSession session,
                             Model model) {
        
        String role = null;
        String fullName = null;
        boolean loginSuccess = false;
        
        // Hardcode tài khoản STAFF
        if (username.equals("staff") && password.equals("staff123")) {
            role = "STAFF";
            fullName = "Nhân Viên";
            loginSuccess = true;
        }
        // Hardcode tài khoản ADMIN
        else if (username.equals("admin") && password.equals("admin123")) {
            role = "ADMIN";
            fullName = "Quản Trị Viên";
            loginSuccess = true;
        }
        // Hardcode tài khoản CUSTOMER mẫu
        else if (username.equals("customer") && password.equals("customer123")) {
            role = "CUSTOMER";
            fullName = "Khách Hàng";
            loginSuccess = true;
        }
        
        if (loginSuccess) {
            // Lưu thông tin vào session
            session.setAttribute("user", username);
            session.setAttribute("username", username);
            session.setAttribute("userRole", role);
            session.setAttribute("fullName", fullName);
            
            System.out.println("✓ Đăng nhập thành công: " + username + " (Role: " + role + ")");
            
            // Redirect theo role
            if ("STAFF".equals(role)) {
                return "redirect:/staff"; // Vào trang Staff Panel trước
            } else if ("ADMIN".equals(role)) {
                return "redirect:/admin"; // Admin dashboard nếu có
            } else {
                return "redirect:/home"; // Customer về trang chủ
            }
        }
        
        // Đăng nhập thất bại
        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        return "login";
    }

    // Xử lý logout
    @GetMapping(value = "/logout")
    public String logout(HttpSession session) {
        session.invalidate(); 
        return "redirect:/";
    }

    @GetMapping(value = "change-password")
    public String changePassword(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "change_password";
    }
}