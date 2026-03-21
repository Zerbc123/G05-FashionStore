package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.service.CartService;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
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
        try {
            String email = (String) session.getAttribute("user");
            if (email == null) {
                throw new RuntimeException("Bạn chưa đăng nhập!");
            }
            
            System.out.println("Finding customer for email: " + email);
            
            Optional<Account> accountOpt = accountRepository.findByEmail(email);
            if (accountOpt.isEmpty()) {
                throw new RuntimeException("Không tìm thấy tài khoản!");
            }
            
            Account account = accountOpt.get();
            System.out.println("Found account: " + account.getAccountId() + ", Role: " + account.getRole().getRoleName());
            
            if (account.getCustomers() == null || account.getCustomers().isEmpty()) {
                throw new RuntimeException("Không tìm thấy thông tin khách hàng!");
            }
            
            Customer customer = account.getCustomers().get(0);
            System.out.println("Found customer: " + customer.getCustomerId());
            
            return customer;
        } catch (Exception e) {
            System.err.println("Error getting current customer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy thông tin khách hàng: " + e.getMessage(), e);
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String viewCart(Model model, HttpSession session) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                return "redirect:/login";
            }
            
            List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
            double total = cartService.getCartTotal(cartItems);

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);
            
            // Cập nhật số lượng giỏ hàng vào session
            session.setAttribute("cartCount", cartItems != null ? cartItems.size() : 0);
            
            return "cart";
        } catch (RuntimeException e) {
            return "redirect:/login";
        }
    }

    @PostMapping("/add")
    @Transactional
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam("sizeId") Integer sizeId,
                            @RequestParam("colorId") Integer colorId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                return "redirect:/login";
            }

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
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/login";
        }
        return "redirect:/cart";
    }

    @PostMapping("/buy-now")
    @Transactional
    public String buyNow(@RequestParam("productId") Long productId,
                         @RequestParam("sizeId") Integer sizeId,
                         @RequestParam("colorId") Integer colorId,
                         @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                return "redirect:/login";
            }
            // Tìm variant dựa trên productId, sizeId và colorId
            Integer variantId = cartService.findVariantByProductSizeColor(productId, sizeId, colorId);
            

            if (variantId == null) {

                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm với size và màu đã chọn!");
                return "redirect:/products/detail/" + productId;
            }
            
            // Tạo giỏ hàng tạm thời chỉ chứa sản phẩm được chọn
            session.setAttribute("buyNowItems", List.of(
                Map.of("variantId", variantId, "quantity", quantity)
            ));
            session.setAttribute("isBuyNow", true);
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
            return "redirect:/order/checkout";
            
        } catch (RuntimeException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/login";
        }
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
        if (customer == null) {
            session.setAttribute("cartCount", 0);
            return;
        }
        
        List<CartItem> cartItems = cartService.getCartItems(customer);
        session.setAttribute("cartCount", cartItems != null ? cartItems.size() : 0);
    }

}
