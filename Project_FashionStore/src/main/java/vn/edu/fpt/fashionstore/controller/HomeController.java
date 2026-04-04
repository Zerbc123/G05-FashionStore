package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.BannerRepository;
import vn.edu.fpt.fashionstore.repository.ProductRepository;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.CartService;
import vn.edu.fpt.fashionstore.service.OrderService;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.WishlistService;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.fashionstore.util.PhoneUtils;
import vn.edu.fpt.fashionstore.util.DateUtils;
import vn.edu.fpt.fashionstore.util.AddressUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class HomeController {

    private final AccountService accountService;
    private final ProductService productService;
    private final ProductRepository productRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WishlistService wishlistService;

    public HomeController(AccountService accountService, ProductService productService,
            ProductRepository productRepository) {
        this.accountService = accountService;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    @Transactional(readOnly = true)
    public String homePage(Model model, @AuthenticationPrincipal OAuth2User principal, HttpSession session) {

        List<vn.edu.fpt.fashionstore.entity.Banner> banners = bannerRepository
                .findByIsActiveTrueOrderByDisplayOrderAsc();
        model.addAttribute("banners", banners);

        // 2. TẠO LOGIC LẤY 4 SẢN PHẨM BÁN CHẠY NHẤT
        // PageRequest.of(0, 4) nghĩa là lấy trang đầu tiên (index 0), và chỉ lấy tối đa
        // 4 phần tử
        Pageable topFour = PageRequest.of(0, 4);
        List<ProductRepository.ProductHomeInfo> bestSellingProducts = productRepository.findTopSellingProducts(topFour);

        // Đẩy danh sách này sang HTML
        model.addAttribute("bestSellingProducts", bestSellingProducts);

        List<ProductRepository.ProductHomeInfo> products = productRepository.getAllProductHome();
        model.addAttribute("products", products);

        String email = (String) session.getAttribute("user");
        if (email == null && principal != null) {
            email = principal.getAttribute("email");
            if (email != null) {
                session.setAttribute("user", email);
            }
        }
        
        // 3. Lấy danh sách ID sản phẩm trong wishlist (nếu đã login)
        if (email != null) {
            Customer customer = accountService.findCustomerByEmail(email);
            if (customer != null) {
                List<Wishlist> wishlist = wishlistService.getWishlistByCustomer(customer);
                java.util.Set<Long> wishlistProductIds = wishlist.stream()
                        .map(w -> w.getProduct().getProductId())
                        .collect(java.util.stream.Collectors.toSet());
                model.addAttribute("wishlistProductIds", wishlistProductIds);
            }
        }

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

            // Update cart count for session
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

}
