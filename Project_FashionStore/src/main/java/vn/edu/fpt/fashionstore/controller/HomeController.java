package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.service.AccountService;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Controller
public class HomeController {

    private final AccountService accountService;

    public HomeController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/home")
    public String homePage(Model model, @AuthenticationPrincipal OAuth2User principal, HttpSession session) {
        // 1. Lấy email từ Session hoặc Google
        String email = (String) session.getAttribute("user");

        if (email == null && principal != null) {
            email = principal.getAttribute("email");
            session.setAttribute("user", email);
        }

        if (email == null) return "redirect:/login";

        // 2. Tìm tài khoản trong DB
        Optional<Account> accountOpt = accountService.findByEmail(email);

        if (accountOpt.isPresent()) {
            // --- HÀNH ĐỘNG: LOGIN (Đã có tài khoản) ---
            Account account = accountOpt.get();
            String roleName = (account.getRole() != null) ? account.getRole().getRoleName() : "Customer";

            session.setAttribute("userName", account.getFullName());
            session.setAttribute("userRole", roleName);

            // 3. KIỂM TRA THÔNG TIN (Dành cho User Google)
            // Kiểm tra nếu là OAuth2 user bằng cách xem password là null hoặc không có hash prefix
            boolean isOAuthUser = account.getPassword() == null || 
                                 account.getPassword().equals("OAUTH2_USER") ||
                                 !account.getPassword().startsWith("$2");

            if (isOAuthUser && "Customer".equalsIgnoreCase(roleName)) {
                boolean hasNoAddress = true;
                if (account.getCustomers() != null && !account.getCustomers().isEmpty()) {
                    String addr = account.getCustomers().get(0).getAddress();
                    if (addr != null && !addr.trim().isEmpty()) {
                        hasNoAddress = false;
                    }
                }

                // Nếu thiếu Phone hoặc Address thì bắt điền (dù là login lần 1 hay lần n)
                if (account.getPhone() == null || hasNoAddress) {
                    return "redirect:/edit-profile?firstLogin=true";
                }
            }

            model.addAttribute("userName", account.getFullName());
            return "page"; // Đăng nhập thành công, vào trang chủ
        }
        else if (principal != null) {
            // --- HÀNH ĐỘNG: REGISTER (Chưa có tài khoản) ---
            // Email Google này chưa có trong DB -> Tự động tạo Acc
            accountService.registerAccount(email, "OAUTH2_USER", principal.getAttribute("name"), "");

            // Tạo xong thì dắt đi điền SĐT và Địa chỉ luôn
            return "redirect:/edit-profile?firstLogin=true";
        }

        return "redirect:/login";
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

    @GetMapping("/profile")
    public String viewProfilePage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        Account acc = accountService.findByEmail(email).orElse(null);

        if (acc != null) {
            // TÊN BIẾN Ở ĐÂY PHẢI KHỚP VỚI TRONG HTML
            model.addAttribute("account", acc);

            Customer cus = (acc.getCustomers() != null && !acc.getCustomers().isEmpty())
                    ? acc.getCustomers().get(0) : new Customer();
            model.addAttribute("customer", cus);

            return "profile";
        }

        return "redirect:/login";
    }

    @GetMapping(value = "/edit-profile")
    public String editProfilePage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        accountService.findByEmail(email).ifPresent(acc -> {
            model.addAttribute("account", acc); // Chứa FullName, Email, Phone

            // Lấy thông tin khách hàng để lấy Address
            Customer customer = (acc.getCustomers() != null && !acc.getCustomers().isEmpty())
                    ? acc.getCustomers().get(0) : new Customer();
            model.addAttribute("customer", customer); // Chứa Address
        });

        return "editprofile";
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


    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes ra) {

        // Gọi hàm authenticate của bạn (so sánh mật khẩu chữ thường)
        Account account = accountService.authenticate(username, password);

        if (account != null) {
            String roleName = (account.getRole() != null) ? account.getRole().getRoleName() : "Customer";

            // LƯU Ý: Ở trang home bạn đang lấy email ra, nên ở đây phải lưu Email vào session
            session.setAttribute("user", account.getEmail());
            session.setAttribute("userRole", roleName);
            session.setAttribute("userName", account.getFullName());

            if ("Admin".equalsIgnoreCase(roleName)) return "redirect:/admin";
            if ("Staff".equalsIgnoreCase(roleName)) return "redirect:/staff";

            return "redirect:/home";
        }

        // Nếu sai, truyền lỗi qua FlashAttribute (không bị mất khi redirect)
        ra.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        session.invalidate(); // Xóa session của app Duy

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        return "redirect:/login?logout"; // Quay về trang login của Duy thôi
    }

    @GetMapping(value = "change-password")
    public String changePassword(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "change_password";
    }

    // Xử lý đăng ký
    @PostMapping(value = "/register")
    public String handleRegister(@RequestParam String firstName,
                                 @RequestParam String lastName,
                                 @RequestParam String email,
                                 @RequestParam String phone,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        // 1. Kiểm tra mật khẩu khớp nhau
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register";
        }

        // 2. Kiểm tra định dạng Email (Regex)
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            ra.addFlashAttribute("error", "Email không đúng định dạng!");
            return "redirect:/register";
        }

        // 3. Chuẩn hóa và kiểm tra SĐT
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() < 10 || cleanPhone.length() > 11) {
            ra.addFlashAttribute("error", "Số điện thoại phải từ 10-11 số!");
            return "redirect:/register";
        }

        // 4. Kiểm tra trùng Email/SĐT trong DB (Sử dụng Service)
        if (accountService.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email này đã được đăng ký!");
            return "redirect:/register";
        }

        // Nếu Service của ông có hàm check phone thì thêm vào
        // if (accountService.existsByPhone(cleanPhone)) { ... }

        String fullName = firstName.trim() + " " + lastName.trim();
        Account newAccount = accountService.registerAccount(email, password, fullName, cleanPhone);

        if (newAccount != null) {
            // Đăng ký thành công -> Tự động đăng nhập (SecurityContextHolder)
            session.setAttribute("user", newAccount.getEmail());
            session.setAttribute("userName", newAccount.getFullName());

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    newAccount.getEmail(), null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            return "redirect:/home";
        } else {
            ra.addFlashAttribute("error", "Lỗi hệ thống, không thể tạo tài khoản!");
            return "redirect:/register";
        }
    }

    @PostMapping("/update-profile")
    public String handleUpdateProfile(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam String gender,
            // Thêm required = false để tránh lỗi khi người dùng không chọn ngày
            @RequestParam(value = "dateOfBirth", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateOfBirth,
            HttpSession session) {

        String email = (String) session.getAttribute("user");

        // Nếu email null (hết hạn session) thì đá về login
        if (email == null) return "redirect:/login";

        accountService.updateProfile(email, fullName, phone, address, gender, dateOfBirth);

        return "redirect:/home";
    }

}