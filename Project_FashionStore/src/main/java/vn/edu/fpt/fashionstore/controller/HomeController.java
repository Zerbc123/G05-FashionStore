package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.repository.ProductRepository;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.CartService;
import vn.edu.fpt.fashionstore.service.ProductService;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final AccountService accountService;
    private final ProductService productService;
    private final ProductRepository productRepository;

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;
    
    @Autowired
    private CartService cartService;

    public HomeController(AccountService accountService, ProductService productService, ProductRepository productRepository) {
        this.accountService = accountService;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @GetMapping("/home")
    public String homePage(Model model, @AuthenticationPrincipal OAuth2User principal, HttpSession session) {
        // Gọi hàm lấy dữ liệu giao diện
        List<ProductRepository.ProductHomeInfo> products = productRepository.getAllProductHome();

        // Đẩy sang Thymeleaf
        model.addAttribute("products", products);

        // 1. Lấy email từ Session hoặc Google
        String email = (String) session.getAttribute("user");

        if (email == null && principal != null) {
            email = principal.getAttribute("email");
            isGoogleLogin = true;
            // Luôn cập nhật session với email mới từ Google
            session.setAttribute("user", email);
        }

        // --- SỬA TẠI ĐÂY ---
        if (email == null) {
            // Nếu là Guest (chưa login), cho họ xem trang chủ luôn
            model.addAttribute("userName", "Guest");
            return "page";
        }

        // 2. Tìm tài khoản trong DB
        Optional<Account> accountOpt = accountService.findByEmail(email);

        if (accountOpt.isPresent()) {
            // --- HÀNH ĐỘNG: LOGIN (Đã có tài khoản) ---
            Account account = accountOpt.get();
            String roleName = (account.getRole() != null) ? account.getRole().getRoleName() : "Customer";

            // Kiểm tra status account
            if (!"active".equalsIgnoreCase(account.getStatus())) {
                // Clear session và redirect về login nếu account không active
                session.invalidate();
                return "redirect:/login?error=account_disabled";
            }

            session.setAttribute("userName", account.getFullName());
            session.setAttribute("userRole", roleName);
            
            // Cập nhật số lượng giỏ hàng vào session
            updateCartCountForCustomer(session, account);

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
    }

    @GetMapping(value = "/login")
    public String loginPage(){

        return "login";
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

        // Force reload account from database to get latest data
        Optional<Account> accountOpt = accountService.findByEmail(email);
        
        if (accountOpt.isPresent()) {
            Account acc = accountOpt.get();
            model.addAttribute("account", acc); // Chứa FullName, Email, Phone

            // Lấy thông tin khách hàng để lấy Address
            Customer customer = (acc.getCustomers() != null && !acc.getCustomers().isEmpty())
                    ? acc.getCustomers().get(0) : new Customer();
            model.addAttribute("customer", customer); // Chứa Address
        } else {
            // Nếu không tìm thấy account, redirect về login
            return "redirect:/login?error=account_not_found";
        }

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
            
            // Cập nhật số lượng giỏ hàng vào session
            updateCartCountForCustomer(session, account);

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



    @PostMapping("/register")
    public String handleRegister(@RequestParam String firstName,
                                 @RequestParam String lastName,
                                 @RequestParam String email,
                                 @RequestParam String phone,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        // 1. Kiểm tra mật khẩu khớp
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register";
        }

        // 2. Kiểm tra email đã tồn tại chưa
        if (accountService.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email này đã được đăng ký!");
            return "redirect:/register";
        }

        // 3. Tạo OTP ngẫu nhiên (6 số)
        String otp = String.valueOf((int) ((Math.random() * 899999) + 100000));

        // 4. LƯU TẤT CẢ THÔNG TIN VÀO SESSION
        // Chúng ta lưu riêng rẽ để tí nữa lấy ra truyền vào hàm registerAccount
        session.setAttribute("tempFirstName", firstName);
        session.setAttribute("tempLastName", lastName);
        session.setAttribute("tempEmail", email);
        session.setAttribute("tempPhone", phone);
        session.setAttribute("tempPass", password);
        session.setAttribute("otpCode", otp);

        // CHÈN THÊM DÒNG NÀY: Lưu thời điểm tạo OTP (miligiây)
        session.setAttribute("otpTimestamp", System.currentTimeMillis());

        // 5. GỬI EMAIL
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác nhận đăng ký - Fashion Store");
            message.setText("Chào " + firstName + ",\n\nMã OTP để hoàn tất đăng ký tài khoản của bạn là: " + otp);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Lỗi gửi mail: " + e.getMessage());
            return "redirect:/register";
        }

        // Chuyển sang trang nhập OTP
        return "redirect:/verify-otp";
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

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam String otp, HttpSession session, RedirectAttributes ra) {
        String serverOtp = (String) session.getAttribute("otpCode");
        Long otpTimestamp = (Long) session.getAttribute("otpTimestamp");

        // 1. KIỂM TRA HẾT HẠN (60000ms = 1 phút)
        if (otpTimestamp == null || (System.currentTimeMillis() - otpTimestamp) > 30000) {
            // Xóa các session tạm để giải phóng bộ nhớ
            session.removeAttribute("otpCode");
            session.removeAttribute("otpTimestamp");

            ra.addFlashAttribute("error", "Mã OTP đã hết hạn (30 giây). Vui lòng thực hiện đăng ký lại!");
            return "redirect:/register";
        }

        // 2. KIỂM TRA MÃ ĐÚNG/SAI
        if (serverOtp != null && serverOtp.equals(otp)) {
            // OTP ĐÚNG -> Lấy thông tin từ session ra để tạo account thực sự
            String firstName = (String) session.getAttribute("tempFirstName");
            String lastName = (String) session.getAttribute("tempLastName");
            String email = (String) session.getAttribute("tempEmail");
            String phone = (String) session.getAttribute("tempPhone");
            String password = (String) session.getAttribute("tempPass");

            String fullName = firstName.trim() + " " + lastName.trim();

            // GỌI SERVICE ĐỂ LƯU VÀO DB
            vn.edu.fpt.fashionstore.entity.Account newAccount = accountService.registerAccount(email, password, fullName, phone);

            if (newAccount != null) {
                // Đăng ký thành công -> Tự động đăng nhập luôn cho user
                session.setAttribute("user", newAccount.getEmail());
                session.setAttribute("userName", newAccount.getFullName());

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        newAccount.getEmail(), null, java.util.Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Xóa sạch sẽ các session tạm sau khi hoàn tất
                session.removeAttribute("otpCode");
                session.removeAttribute("otpTimestamp"); // Xóa luôn timestamp
                session.removeAttribute("tempFirstName");
                session.removeAttribute("tempLastName");
                session.removeAttribute("tempEmail");
                session.removeAttribute("tempPhone");
                session.removeAttribute("tempPass");

                return "redirect:/home";
            } else {
                ra.addFlashAttribute("error", "Lỗi hệ thống khi tạo tài khoản!");
                return "redirect:/register";
            }
        } else {
            ra.addFlashAttribute("error", "Mã OTP không chính xác, vui lòng kiểm tra lại!");
            return "redirect:/verify-otp";
        }
    }

    // THÊM HÀM NÀY VÀO ĐỂ HIỂN THỊ TRANG NHẬP OTP
    @GetMapping("/verify-otp")
    public String viewOtpPage(HttpSession session) {
        // Kiểm tra xem có đang trong quá trình đăng ký không (tránh người dùng gõ bừa link)
        if (session.getAttribute("otpCode") == null) {
            return "redirect:/register";
        }
        return "verifyOTP"; // Tên file HTML của bạn (verify-otp.html)
    }

    // 1. Trang đăng ký sạch sẽ ban đầu (Không có thông báo lỗi)
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/register-expired")
    public String registerExpiredPage(Model model) {
        model.addAttribute("error", "Mã xác thực đã hết hạn sau 30 giây. Vui lòng đăng ký lại!");
        return "register";
    }
    
    // Helper method để cập nhật cart count vào session
    private void updateCartCountForCustomer(HttpSession session, Account account) {
        try {
            if (account.getCustomers() != null && !account.getCustomers().isEmpty()) {
                Customer customer = account.getCustomers().get(0);
                List<CartItem> cartItems = cartService.getCartItems(customer);
                session.setAttribute("cartCount", cartItems.size());
            } else {
                session.setAttribute("cartCount", 0);
            }
        } catch (Exception e) {
            // Nếu có lỗi, set cartCount = 0
            session.setAttribute("cartCount", 0);
        }
    }

}