package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.service.CartService;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private AccountRepository accountRepository;

    // Lấy customer từ session
    private Customer getCurrentCustomer(HttpSession session) {
        String email = (String) session.getAttribute("user");
        if (email == null) {
            throw new RuntimeException("Bạn chưa đăng nhập!");
        }
        
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy tài khoản!");
        }
        
        Account account = accountOpt.get();
        if (account.getCustomers() == null || account.getCustomers().isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin khách hàng!");
        }
        
        return account.getCustomers().get(0);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String viewCart(Model model, HttpSession session) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
            double total = cartService.getCartTotal(cartItems);

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);
            
            // Cập nhật số lượng giỏ hàng vào session
            session.setAttribute("cartCount", cartItems.size());
            
            return "cart";
        } catch (RuntimeException e) {
            return "redirect:/login";
        }
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam("sizeId") Integer sizeId,
                            @RequestParam("colorId") Integer colorId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            // Tìm variant dựa trên productId, sizeId và colorId
            Integer variantId = cartService.findVariantByProductSizeColor(productId, sizeId, colorId);
            
            if (variantId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm với size và màu đã chọn!");
                return "redirect:/products/detail/" + productId;
            }
            
            cartService.addToCart(currentCustomer, variantId, quantity);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, currentCustomer);
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/login";
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateCart(@RequestParam("cartItemId") Integer cartItemId,
                             @RequestParam("action") String action,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            // Lấy cart item hiện tại
            CartItem cartItem = cartService.getCartItemById(cartItemId);
            if (cartItem == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm trong giỏ hàng!");
                return "redirect:/cart";
            }
            
            int newQuantity = cartItem.getQuantity();
            if ("increase".equals(action)) {
                newQuantity++;
            } else if ("decrease".equals(action) && newQuantity > 1) {
                newQuantity--;
            }
            
            cartService.updateQuantity(cartItemId, newQuantity);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, getCurrentCustomer(session));
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam("cartItemId") Integer cartItemId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.removeFromCart(cartItemId);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, getCurrentCustomer(session));
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/cart";
    }
    
    // Helper method để cập nhật số lượng giỏ hàng vào session
    private void updateCartCount(HttpSession session, Customer customer) {
        List<CartItem> cartItems = cartService.getCartItems(customer);
        session.setAttribute("cartCount", cartItems.size());
    }

    // =======================================================
    // 1. MỞ TRANG CHECKOUT
    // =======================================================
    @GetMapping("/checkout")
    @Transactional(readOnly = true)
    public String checkoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            List<CartItem> cartItems = cartService.getCartItems(currentCustomer);

            // Nếu giỏ hàng trống thì không cho vào trang checkout
            if (cartItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
                return "redirect:/cart";
            }

            double total = cartService.getCartTotal(cartItems);

            // Gửi dữ liệu ra cột bên phải
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);

            // Lấy sẵn tên và sđt từ Customer điền sẵn vào form cho khách lười gõ
            model.addAttribute("fullName", currentCustomer.getFullName());
            
            // Sử dụng chuỗi điện thoại trực tiếp (đã lưu với số 0 đầu nếu có)
            String phoneStr = currentCustomer.getPhone();
            if (phoneStr == null) phoneStr = "";
            model.addAttribute("phone", phoneStr);
            
            model.addAttribute("email", currentCustomer.getEmail());
            model.addAttribute("address", currentCustomer.getAddress());

            return "checkout";
        } catch (RuntimeException e) {
            return "redirect:/login";
        }
    }

    // =======================================================
    // 2. XỬ LÝ KHI BẤM NÚT "ĐẶT HÀNG" (MANUAL VALIDATION)
    // =======================================================
    @PostMapping("/checkout/place-order")
    public String placeOrder(
            @RequestParam(value = "fullName", defaultValue = "") String fullName,
            @RequestParam(value = "phone", defaultValue = "") String phone,
            @RequestParam(value = "address", defaultValue = "") String address,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "deliveryMethod", required = false) String deliveryMethod,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            Model model,
            HttpSession session) {

        boolean hasError = false;

        // Bắt lỗi: Tên rỗng hoặc chứa số/ký tự đặc biệt
        if (fullName.trim().isEmpty()) {
            model.addAttribute("errorFullName", "Vui lòng nhập họ và tên của bạn.");
            hasError = true;
        } else if (!fullName.matches("^[\\p{L}\\s]+$")) {
            // Regex: \p{L} là chữ cái bất kỳ (hỗ trợ tiếng Việt), \s là khoảng trắng
            model.addAttribute("errorFullName", "Họ và tên chỉ được chứa chữ cái, không nhập số hay ký tự đặc biệt.");
            hasError = true;
        }

        // Bắt lỗi: Số điện thoại
        if (phone.trim().isEmpty() || !phone.matches("^0[0-9]{9}$")) {
            model.addAttribute("errorPhone", "Số điện thoại không hợp lệ (Bắt buộc 10 số và bắt đầu bằng 0).");
            hasError = true;
        }

        // Bắt lỗi: Email (Bắt buộc nhập và phải có đuôi .com)
        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("errorEmail", "Vui lòng nhập email.");
            hasError = true;
        } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.com$")) {
            model.addAttribute("errorEmail", "Email không hợp lệ (Phải đúng định dạng và bắt buộc có đuôi .com).");
            hasError = true;
        }

        // Bắt lỗi: Địa chỉ rỗng / định dạng
        if (address.trim().isEmpty() || !vn.edu.fpt.fashionstore.util.AddressUtils.isValid(address)) {
            model.addAttribute("errorAddress", "Địa chỉ không hợp lệ. Vui lòng chọn tỉnh/quận/xã và nhập số nhà, đường.");
            hasError = true;
        }

        // Nếu có lỗi -> Phải Load lại danh sách sản phẩm và trả về trang checkout kèm lỗi
        if (hasError) {
            try {
                Customer currentCustomer = getCurrentCustomer(session);
                List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
                double total = cartService.getCartTotal(cartItems);

                // Gửi lại data giỏ hàng
                model.addAttribute("cartItems", cartItems);
                model.addAttribute("total", total);

                // Giữ nguyên chữ khách đã nhập
                model.addAttribute("fullName", fullName);
                model.addAttribute("phone", phone);
                model.addAttribute("email", email);
                model.addAttribute("address", address);
                model.addAttribute("note", note);

                return "checkout"; // Trả lại trang để khách sửa lỗi
            } catch (Exception e) {
                return "redirect:/login";
            }
        }
        // Về sau bạn sẽ gọi OrderService ở đây để lưu vào DB.
        // Tạm thời redirect về home để đỡ lỗi
        return "redirect:/home";
    }
}
