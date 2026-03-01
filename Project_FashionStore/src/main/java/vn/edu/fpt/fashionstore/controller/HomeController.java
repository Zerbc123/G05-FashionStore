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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.repository.ProductRepository;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.CartService;
import vn.edu.fpt.fashionstore.service.OrderService;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.util.PhoneUtils;
import vn.edu.fpt.fashionstore.util.DateUtils;
import vn.edu.fpt.fashionstore.util.AddressUtils;

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

    @Autowired
    private OrderService orderService;

    public HomeController(AccountService accountService, ProductService productService, ProductRepository productRepository) {
        this.accountService = accountService;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @GetMapping("/home")
    public String homePage(Model model, @AuthenticationPrincipal OAuth2User principal, HttpSession session) {

        List<ProductRepository.ProductHomeInfo> products = productRepository.getAllProductHome();
        model.addAttribute("products", products);

        String email = (String) session.getAttribute("user");
        boolean isGoogleLogin = false;

        if (email == null && principal != null) {
            email = principal.getAttribute("email");
            session.setAttribute("user", email);

            Optional<Account> accOpt = accountService.findByEmail(email);
            if (accOpt.isPresent()) {
                Account acc = accOpt.get();
                session.setAttribute("userRole", acc.getRole().getRoleName());
                session.setAttribute("userName", acc.getFullName());
            }
        }

        if (email == null) {
            model.addAttribute("userName", "Guest");
            return "page";
        }

        Optional<Account> accountOpt = accountService.findByEmail(email);

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            String roleName = (account.getRole() != null) ? account.getRole().getRoleName() : "Customer";

            if (!"active".equalsIgnoreCase(account.getStatus())) {
                session.invalidate();
                return "redirect:/login?error=account_disabled";
            }

            session.setAttribute("userName", account.getFullName());
            session.setAttribute("userRole", roleName);

            updateCartCountForCustomer(session, account);

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

                String ph = account.getPhone();
                if (ph == null || ph.isEmpty() || hasNoAddress) {
                    return "redirect:/edit-profile?firstLogin=true";
                }
            }

            model.addAttribute("userName", account.getFullName());
            return "page";
        } else if (principal != null) {
            accountService.registerAccount(email, "OAUTH2_USER", principal.getAttribute("name"), "");
            return "redirect:/edit-profile?firstLogin=true";
        }

        return "login";
    }

    @GetMapping("/profile")
    public String viewProfilePage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        Account acc = accountService.findByEmail(email).orElse(null);

        if (acc != null) {
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

        Optional<Account> accountOpt = accountService.findByEmail(email);

        if (accountOpt.isPresent()) {
            Account acc = accountOpt.get();
            model.addAttribute("account", acc);

            Customer customer = (acc.getCustomers() != null && !acc.getCustomers().isEmpty())
                    ? acc.getCustomers().get(0) : new Customer();
            model.addAttribute("customer", customer);
        } else {
            return "redirect:/login?error=account_not_found";
        }

        return "editprofile";
    }

    @GetMapping(value = "/order-history")
    public String orderHistoryPage(HttpSession session, Model model) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        try {
            // Lấy customer hiện tại
            Customer currentCustomer = getCurrentCustomer(session);
            
            // Kiểm tra nếu customer không tồn tại
            if (currentCustomer == null) {
                return "redirect:/login";
            }
            
            // Lấy danh sách đơn hàng của customer
            List<Order> customerOrders = orderService.getOrdersByCustomer(currentCustomer);
            
            // Thêm vào model
            model.addAttribute("orders", customerOrders);
            model.addAttribute("customerName", currentCustomer.getFullName());
            
            return "viewhistory";
        } catch (Exception e) {
            return "redirect:/login";
        }
    }

    @GetMapping(value = "/wishlist")
    public String wishlistPage(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "wishlist";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes ra) {

        // validate password format (exactly 6 alphanumeric characters)
        if (!vn.edu.fpt.fashionstore.util.PasswordUtils.isValid(password)) {
            ra.addFlashAttribute("error", "Mật khẩu phải gồm 6 ký tự chữ và số, không chứa ký tự đặc biệt!");
            return "redirect:/login";
        }

        Account account = accountService.authenticate(username, password);

        if (account != null) {
            String roleName = (account.getRole() != null) ? account.getRole().getRoleName() : "Customer";

            session.setAttribute("user", account.getEmail());
            session.setAttribute("userRole", roleName);
            session.setAttribute("userName", account.getFullName());

            updateCartCountForCustomer(session, account);

            if ("Admin".equalsIgnoreCase(roleName)) return "redirect:/admin";
            if ("Staff".equalsIgnoreCase(roleName)) return "redirect:/staff";

            return "redirect:/home";
        }

        ra.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        session.invalidate();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        return "redirect:/login?logout";
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

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register";
        }

        if (!vn.edu.fpt.fashionstore.util.PasswordUtils.isValid(password)) {
            ra.addFlashAttribute("error", "Mật khẩu phải gồm 6 ký tự chữ và số, không chứa ký tự đặc biệt!");
            return "redirect:/register";
        }

        if (accountService.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email này đã được đăng ký!");
            return "redirect:/register";
        }

        if (!PhoneUtils.isValid(phone)) {
            ra.addFlashAttribute("error", "Số điện thoại không hợp lệ! Vui lòng nhập số điện thoại 10 chữ số bắt đầu bằng 0.");
            return "redirect:/register";
        }

        String otp = String.valueOf((int) ((Math.random() * 899999) + 100000));

        session.setAttribute("tempFirstName", firstName);
        session.setAttribute("tempLastName", lastName);
        session.setAttribute("tempEmail", email);
        session.setAttribute("tempPhone", phone);
        session.setAttribute("tempPass", password);
        session.setAttribute("otpCode", otp);
        session.setAttribute("otpTimestamp", System.currentTimeMillis());

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

    @PostMapping("/update-profile")
    public String handleUpdateProfile(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam String gender,
            @RequestParam(value = "dateOfBirth", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateOfBirth,
            HttpSession session,
            RedirectAttributes ra) {

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        if (!PhoneUtils.isValid(phone)) {
            ra.addFlashAttribute("error", "Số điện thoại không hợp lệ! Vui lòng nhập số điện thoại 10 chữ số bắt đầu bằng 0.");
            return "redirect:/edit-profile";
        }

        // validate date of birth range (server-side) in addition to front-end min/max
        if (!DateUtils.isValidDOB(dateOfBirth)) {
            ra.addFlashAttribute("error", "Ngày sinh không hợp lệ. Vui lòng chọn ngày từ 01/01/1950 đến hôm nay.");
            return "redirect:/edit-profile";
        }

        if (address == null || address.trim().isEmpty() || !AddressUtils.isValid(address)) {
            ra.addFlashAttribute("error", "Địa chỉ không được để trống và phải chọn tỉnh/ huyện/ xã cùng số nhà.");
            return "redirect:/edit-profile";
        }

        // phone and address both valid, combine is done on client side
        accountService.updateProfile(email, fullName, phone, address, gender, dateOfBirth);

        return "redirect:/home";
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

    @GetMapping("/verify-otp")
    public String viewOtpPage(HttpSession session) {
        if (session.getAttribute("otpCode") == null) {
            return "redirect:/register";
        }
        return "verifyOTP";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/register-expired")
    public String registerExpiredPage(Model model) {
        model.addAttribute("error", "Mã OTP đã hết hạn. Vui lòng đăng ký lại!");
        return "register";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

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

    private Customer getCurrentCustomer(HttpSession session) {
        try {
            String email = (String) session.getAttribute("user");
            if (email == null) return null;
            
            Account account = accountService.findByEmail(email).orElse(null);
            if (account == null || account.getCustomers() == null || account.getCustomers().isEmpty()) {
                return null;
            }
            
            return account.getCustomers().get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
