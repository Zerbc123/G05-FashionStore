package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.CartService;
import vn.edu.fpt.fashionstore.service.WishlistService;
import vn.edu.fpt.fashionstore.util.PhoneUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Autowired
    private CartService cartService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WishlistService wishlistService;

    // ==================== LOGIN ====================

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes ra) {

        // Validate password format for existing users login
        if (!vn.edu.fpt.fashionstore.util.PasswordUtils.isValid(password)) {
            ra.addFlashAttribute("error",
                    "Mật khẩu phải từ 8-12 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 số!");
            return "redirect:/login";
        }

        Account account = accountService.authenticate(username, password);

        if (account != null) {
            String roleName = (account.getRole() != null) ? account.getRole().getRoleName() : "Customer";

            session.setAttribute("user", account.getEmail());
            session.setAttribute("userRole", roleName);
            session.setAttribute("userName", account.getFullName());

            updateCartCountForCustomer(session, account);

            if ("Admin".equalsIgnoreCase(roleName)) {
                return "redirect:/admin";
            }

            if ("Nhân viên bán hàng (Sale)".equalsIgnoreCase(roleName) ||
                    "Quản lý kho (Stock)".equalsIgnoreCase(roleName) ||
                    "Hỗ trợ khách hàng (Support)".equalsIgnoreCase(roleName)) {
                return "redirect:/staff";
            }

            return "redirect:/home";
        }

        // Check specific account status for better error messages
        String accountStatus = accountService.getAccountStatus(username);
        if ("INACTIVE".equalsIgnoreCase(accountStatus)) {
            ra.addFlashAttribute("error",
                    "Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
        } else if ("LOCKED".equalsIgnoreCase(accountStatus)) {
            ra.addFlashAttribute("error",
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
        } else {
            ra.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        }

        return "redirect:/login";
    }

    // ==================== LOGOUT ====================

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        session.invalidate();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        return "redirect:/login?logout";
    }

    // ==================== REGISTER ====================

    @GetMapping("/register")
    public String registerPage() {
        return "register";
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

        // Luôn trả lại các giá trị đã nhập vào form nếu có lỗi (ngoại trừ mật khẩu)
        ra.addFlashAttribute("enteredFirstName", firstName);
        ra.addFlashAttribute("enteredLastName", lastName);
        ra.addFlashAttribute("enteredEmail", email);
        ra.addFlashAttribute("enteredPhone", phone);

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register";
        }

        if (!vn.edu.fpt.fashionstore.util.PasswordUtils.isValid(password)) {
            ra.addFlashAttribute("error",
                    "Mật khẩu phải từ 8-12 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 số, có thể chứa ký tự đặc biệt!");
            return "redirect:/register";
        }

        if (accountService.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email này đã được đăng ký!");
            return "redirect:/register";
        }

        if (!PhoneUtils.isValid(phone)) {
            ra.addFlashAttribute("error",
                    "Số điện thoại không hợp lệ! Vui lòng nhập số điện thoại 10 chữ số bắt đầu bằng 0.");
            return "redirect:/register";
        }

        // Kiểm tra chống spam OTP cho Đăng ký
        Long lastOtpRequestTime = (Long) session.getAttribute("registerLastOtpTime");
        Integer otpRequestCount = (Integer) session.getAttribute("registerOtpCount");

        if (lastOtpRequestTime != null && otpRequestCount != null) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastOtpRequestTime;

            // Tính thời gian chờ: lần đầu 30s, các lần sau: số lần * 60s
            long waitTimeSeconds = (otpRequestCount == 1) ? 30 : (long) otpRequestCount * 60;

            if (timeSinceLastRequest < waitTimeSeconds * 1000) {
                long remainingSeconds = (waitTimeSeconds * 1000 - timeSinceLastRequest) / 1000;
                ra.addFlashAttribute("error",
                        "Vui lòng đợi " + remainingSeconds + " giây nữa trước khi gửi lại OTP đăng ký!");
                return "redirect:/register";
            }
        }

        String otp = String.valueOf((int) ((Math.random() * 899999) + 100000));

        session.setAttribute("tempFirstName", firstName);
        session.setAttribute("tempLastName", lastName);
        session.setAttribute("tempEmail", email);
        session.setAttribute("tempPhone", phone);
        session.setAttribute("tempPass", password);
        session.setAttribute("otpCode", otp);
        session.setAttribute("otpTimestamp", System.currentTimeMillis());

        // Cập nhật thông tin chống spam Đăng ký
        session.setAttribute("registerLastOtpTime", System.currentTimeMillis());
        if (otpRequestCount == null) {
            session.setAttribute("registerOtpCount", 1);
        } else {
            session.setAttribute("registerOtpCount", otpRequestCount + 1);
        }

        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác nhận đăng ký - Fashion Store");
            message.setText("Chào " + firstName + ",\n\nMã OTP của bạn là: " + otp);
            mailSender.send(message);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi gửi mail: " + e.getMessage());
            return "redirect:/register";
        }

        return "redirect:/verify-otp";
    }

    @GetMapping("/register-expired")
    public String registerExpiredPage(Model model) {
        model.addAttribute("error", "Mã OTP đã hết hạn. Vui lòng đăng ký lại!");
        return "register";
    }

    // ==================== OTP VERIFICATION ====================

    @GetMapping("/verify-otp")
    public String viewOtpPage(HttpSession session, Model model) {
        if (session.getAttribute("otpCode") == null) {
            return "redirect:/register";
        }

        Long lastOtpTime = (Long) session.getAttribute("registerLastOtpTime");
        Integer otpCount = (Integer) session.getAttribute("registerOtpCount");
        model.addAttribute("lastOtpTime", lastOtpTime != null ? lastOtpTime : 0L);
        model.addAttribute("otpCount", otpCount != null ? otpCount : 1);

        return "verifyOTP";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam String otp, HttpSession session, RedirectAttributes ra) {
        String serverOtp = (String) session.getAttribute("otpCode");
        Long otpTimestamp = (Long) session.getAttribute("otpTimestamp");

        if (otpTimestamp == null || (System.currentTimeMillis() - otpTimestamp) > 30000) {

            session.removeAttribute("otpCode");
            session.removeAttribute("otpTimestamp");

            ra.addFlashAttribute("error", "Mã OTP đã hết hạn (30 giây). Vui lòng đăng ký lại!");
            return "redirect:/register";
        }

        if (serverOtp != null && serverOtp.equals(otp)) {

            String firstName = (String) session.getAttribute("tempFirstName");
            String lastName = (String) session.getAttribute("tempLastName");
            String email = (String) session.getAttribute("tempEmail");
            String phone = (String) session.getAttribute("tempPhone");
            String password = (String) session.getAttribute("tempPass");

            String fullName = firstName.trim() + " " + lastName.trim();

            Account newAccount = accountService.registerAccount(email, password, fullName, phone);

            if (newAccount != null) {
                session.setAttribute("user", newAccount.getEmail());
                session.setAttribute("userName", newAccount.getFullName());

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        newAccount.getEmail(), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                session.removeAttribute("otpCode");
                session.removeAttribute("otpTimestamp");
                session.removeAttribute("tempFirstName");
                session.removeAttribute("tempLastName");
                session.removeAttribute("tempEmail");
                session.removeAttribute("tempPhone");
                session.removeAttribute("tempPass");
                session.removeAttribute("registerLastOtpTime");
                session.removeAttribute("registerOtpCount");

                return "redirect:/home";
            } else {
                ra.addFlashAttribute("error", "Lỗi hệ thống khi tạo tài khoản!");
                return "redirect:/register";
            }
        } else {
            ra.addFlashAttribute("error", "Mã OTP không chính xác!");
            return "redirect:/verify-otp";
        }
    }

    @PostMapping("/resend-register-otp")
    public String resendRegisterOtp(HttpSession session, RedirectAttributes ra) {
        String email = (String) session.getAttribute("tempEmail");
        String firstName = (String) session.getAttribute("tempFirstName");

        if (email == null || firstName == null) {
            ra.addFlashAttribute("error", "Phiên làm việc đã hết hạn. Vui lòng đăng ký lại!");
            return "redirect:/register";
        }

        // Kiểm tra chống spam OTP cho Đăng ký (luôn đồng bộ với handleRegister)
        Long lastOtpRequestTime = (Long) session.getAttribute("registerLastOtpTime");
        Integer otpRequestCount = (Integer) session.getAttribute("registerOtpCount");

        if (lastOtpRequestTime != null && otpRequestCount != null) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastOtpRequestTime;
            long waitTimeSeconds = (otpRequestCount == 1) ? 30 : (long) otpRequestCount * 60;

            if (timeSinceLastRequest < waitTimeSeconds * 1000) {
                long remainingSeconds = (waitTimeSeconds * 1000 - timeSinceLastRequest) / 1000;
                ra.addFlashAttribute("error", "Vui lòng đợi " + remainingSeconds + " giây nữa trước khi gửi lại OTP!");
                return "redirect:/verify-otp";
            }
        }

        try {
            // Tạo OTP mới
            String otp = String.valueOf((int) ((Math.random() * 899999) + 100000));
            session.setAttribute("otpCode", otp);
            session.setAttribute("otpTimestamp", System.currentTimeMillis());

            // Cập nhật thông tin chống spam
            session.setAttribute("registerLastOtpTime", System.currentTimeMillis());
            session.setAttribute("registerOtpCount", (otpRequestCount != null) ? otpRequestCount + 1 : 1);

            // Gửi email
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác nhận đăng ký mới - Fashion Store");
            message.setText("Chào " + firstName + ",\n\nMã OTP mới của bạn là: " + otp);
            mailSender.send(message);

            ra.addFlashAttribute("success", "Mã OTP mới đã được gửi đến email của bạn!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi gửi mail: " + e.getMessage());
        }

        return "redirect:/verify-otp";
    }

    // ==================== FORGOT PASSWORD ====================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendForgotPasswordOtp(@RequestParam String email, HttpSession session, RedirectAttributes ra) {
        try {
            // Kiểm tra email tồn tại trong hệ thống
            Optional<Account> accountOpt = accountService.findByEmail(email);
            if (!accountOpt.isPresent()) {
                ra.addFlashAttribute("error", "Email không tồn tại trong hệ thống!");
                return "redirect:/forgot-password";
            }

            // Kiểm tra chống spam OTP
            Long lastOtpRequestTime = (Long) session.getAttribute("lastOtpRequestTime");
            Integer otpRequestCount = (Integer) session.getAttribute("otpRequestCount");

            if (lastOtpRequestTime != null && otpRequestCount != null) {
                long timeSinceLastRequest = System.currentTimeMillis() - lastOtpRequestTime;

                // Tính thời gian chờ: 30s, 1p, 2p, 3p, 4p, 5p...
                long waitTimeSeconds;
                if (otpRequestCount == 1) {
                    waitTimeSeconds = 30; // Lần đầu: 30 giây
                } else {
                    waitTimeSeconds = otpRequestCount * 60; // Các lần sau: 1p, 2p, 3p...
                }

                if (timeSinceLastRequest < waitTimeSeconds * 1000) {
                    long remainingSeconds = (waitTimeSeconds * 1000 - timeSinceLastRequest) / 1000;
                    ra.addFlashAttribute("error",
                            "Vui lòng đợi " + remainingSeconds + " giây nữa trước khi gửi lại OTP!");
                    return "redirect:/forgot-password";
                }
            }

            // Tạo OTP
            String otp = String.valueOf((int) ((Math.random() * 899999) + 100000));

            // Lưu thông tin vào session
            session.setAttribute("resetEmail", email);
            session.setAttribute("resetOtpCode", otp);
            session.setAttribute("resetOtpTimestamp", System.currentTimeMillis());

            // Cập nhật thông tin chống spam
            session.setAttribute("lastOtpRequestTime", System.currentTimeMillis());
            if (otpRequestCount == null) {
                session.setAttribute("otpRequestCount", 1);
            } else {
                session.setAttribute("otpRequestCount", otpRequestCount + 1);
            }

            // Gửi email
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác nhận đặt lại mật khẩu - Fashion Store");
            message.setText("Mã OTP đặt lại mật khẩu của bạn là: " + otp + ". Mã có hiệu lực trong 5 phút.");
            mailSender.send(message);

            ra.addFlashAttribute("success", "Mã OTP đã được gửi đến email của bạn!");
            return "redirect:/reset-password";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi gửi email: " + e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    // ==================== RESET PASSWORD ====================

    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session, Model model) {
        if (session.getAttribute("resetOtpCode") == null) {
            return "redirect:/forgot-password";
        }
        Long lastOtpTime = (Long) session.getAttribute("lastOtpRequestTime");
        Integer otpCount = (Integer) session.getAttribute("otpRequestCount");
        model.addAttribute("lastOtpTime", lastOtpTime != null ? lastOtpTime : 0L);
        model.addAttribute("otpCount", otpCount != null ? otpCount : 1);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String otp,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {
        String serverOtp = (String) session.getAttribute("resetOtpCode");
        Long otpTimestamp = (Long) session.getAttribute("resetOtpTimestamp");
        String email = (String) session.getAttribute("resetEmail");

        // Kiểm tra OTP có hết hạn không (5 phút)
        if (otpTimestamp == null || (System.currentTimeMillis() - otpTimestamp) > 300000) {
            session.removeAttribute("resetOtpCode");
            session.removeAttribute("resetOtpTimestamp");
            session.removeAttribute("resetEmail");
            session.removeAttribute("lastOtpRequestTime");
            session.removeAttribute("otpRequestCount");
            ra.addFlashAttribute("error", "Mã OTP đã hết hạn (5 phút). Vui lòng thử lại!");
            return "redirect:/forgot-password";
        }

        // Kiểm tra OTP có đúng không
        if (serverOtp == null || !serverOtp.equals(otp)) {
            ra.addFlashAttribute("error", "Mã OTP không chính xác!");
            return "redirect:/reset-password";
        }

        // Kiểm tra mật khẩu có khớp không
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp!");
            return "redirect:/reset-password";
        }

        // Kiểm tra định dạng mật khẩu
        if (!vn.edu.fpt.fashionstore.util.PasswordUtils.isValid(newPassword)) {
            ra.addFlashAttribute("error",
                    "Mật khẩu phải từ 8-12 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 số, có thể chứa ký tự đặc biệt!");
            return "redirect:/reset-password";
        }

        try {
            // Cập nhật mật khẩu
            Optional<Account> accountOpt = accountService.findByEmail(email);
            if (!accountOpt.isPresent()) {
                session.removeAttribute("resetOtpCode");
                session.removeAttribute("resetOtpTimestamp");
                session.removeAttribute("resetEmail");
                session.removeAttribute("lastOtpRequestTime");
                session.removeAttribute("otpRequestCount");
                ra.addFlashAttribute("error", "Tài khoản không tồn tại. Vui lòng thử lại!");
                return "redirect:/forgot-password";
            }

            Account account = accountOpt.get();
            account.setPassword(passwordEncoder.encode(newPassword));
            accountService.saveAccount(account);

            // Xóa session
            session.removeAttribute("resetOtpCode");
            session.removeAttribute("resetOtpTimestamp");
            session.removeAttribute("resetEmail");
            session.removeAttribute("lastOtpRequestTime");
            session.removeAttribute("otpRequestCount");

            ra.addFlashAttribute("success", "Mật khẩu đã được cập nhật thành công! Vui lòng đăng nhập.");
            return "redirect:/login";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi cập nhật mật khẩu: " + e.getMessage());
            return "redirect:/reset-password";
        }
    }

    @PostMapping("/resend-otp")
    public String resendForgotPasswordOtp(HttpSession session, RedirectAttributes ra) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }

        // Kiểm tra chống spam OTP
        Long lastOtpRequestTime = (Long) session.getAttribute("lastOtpRequestTime");
        Integer otpRequestCount = (Integer) session.getAttribute("otpRequestCount");

        if (lastOtpRequestTime != null && otpRequestCount != null) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastOtpRequestTime;
            long waitTimeSeconds = (otpRequestCount == 1) ? 30 : (long) otpRequestCount * 60;

            if (timeSinceLastRequest < waitTimeSeconds * 1000) {
                long remainingSeconds = (waitTimeSeconds * 1000 - timeSinceLastRequest) / 1000;
                ra.addFlashAttribute("error", "Vui lòng đợi " + remainingSeconds + " giây nữa trước khi gửi lại OTP!");
                return "redirect:/reset-password";
            }
        }

        try {
            String otp = String.valueOf((int) ((Math.random() * 899999) + 100000));

            session.setAttribute("resetOtpCode", otp);
            session.setAttribute("resetOtpTimestamp", System.currentTimeMillis());
            session.setAttribute("lastOtpRequestTime", System.currentTimeMillis());
            if (otpRequestCount == null) {
                session.setAttribute("otpRequestCount", 1);
            } else {
                session.setAttribute("otpRequestCount", otpRequestCount + 1);
            }

            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác nhận đặt lại mật khẩu - Fashion Store");
            message.setText("Mã OTP đặt lại mật khẩu của bạn là: " + otp + ". Mã có hiệu lực trong 5 phút.");
            mailSender.send(message);

            ra.addFlashAttribute("success", "Mã OTP mới đã được gửi đến email của bạn!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi gửi email: " + e.getMessage());
        }

        return "redirect:/reset-password";
    }

    // ==================== PROFILE MANAGEMENT ====================

    @GetMapping("/profile")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String viewProfilePage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("user");
        if (email == null)
            return "redirect:/login";

        Account acc = accountService.findByEmail(email).orElse(null);

        if (acc != null) {
            model.addAttribute("account", acc);

            // Fix lazy loading by using AccountService.findCustomerByEmail
            Customer cus = accountService.findCustomerByEmail(email);
            if (cus == null) {
                cus = new Customer();
            }
            model.addAttribute("customer", cus);

            // Lấy số lượng wishlist cho customer
            long wishlistCount = 0;
            if (cus.getCustomerId() != null) {
                wishlistCount = getWishlistCount(cus);
            }
            model.addAttribute("wishlistCount", wishlistCount);

            return "profile";
        }
        return "redirect:/login";
    }

    @GetMapping(value = "/edit-profile")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String editProfilePage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("user");
        if (email == null)
            return "redirect:/login";

        Optional<Account> accountOpt = accountService.findByEmail(email);

        if (accountOpt.isPresent()) {
            Account acc = accountOpt.get();
            model.addAttribute("account", acc);

            Customer customer = (acc.getCustomers() != null && !acc.getCustomers().isEmpty())
                    ? acc.getCustomers().get(0)
                    : new Customer();
            model.addAttribute("customer", customer);
        } else {
            return "redirect:/login?error=account_not_found";
        }

        return "editprofile";
    }

    @PostMapping("/update-profile")
    public String handleUpdateProfile(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam String gender,
            @RequestParam(value = "dateOfBirth", required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate dateOfBirth,
            HttpSession session,
            RedirectAttributes ra) {

        String email = (String) session.getAttribute("user");
        if (email == null)
            return "redirect:/login";

        if (!PhoneUtils.isValid(phone)) {
            ra.addFlashAttribute("error",
                    "Số điện thoại không hợp lệ! Vui lòng nhập số điện thoại 10 chữ số bắt đầu bằng 0.");
            return "redirect:/edit-profile";
        }

        // validate date of birth range (server-side) in addition to front-end min/max
        if (!vn.edu.fpt.fashionstore.util.DateUtils.isValidDOB(dateOfBirth)) {
            ra.addFlashAttribute("error", "Ngày sinh không hợp lệ. Vui lòng chọn ngày từ 01/01/1950 đến hôm nay.");
            return "redirect:/edit-profile";
        }

        if (address == null || address.trim().isEmpty() || !vn.edu.fpt.fashionstore.util.AddressUtils.isValid(address)) {
            ra.addFlashAttribute("error", "Địa chỉ không được để trống và phải chọn tỉnh/ huyện/ xã cùng số nhà.");
            return "redirect:/edit-profile";
        }

        // phone and address both valid, combine is done on client side
        accountService.updateProfile(email, fullName, phone, address, gender, dateOfBirth);

        return "redirect:/home";
    }

    // ==================== HELPER METHODS ====================

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
            session.setAttribute("cartCount", 0);
        }
    }

    private long getWishlistCount(Customer customer) {
        try {
            return wishlistService.getWishlistCount(customer);
        } catch (Exception e) {
            return 0;
        }
    }
}
