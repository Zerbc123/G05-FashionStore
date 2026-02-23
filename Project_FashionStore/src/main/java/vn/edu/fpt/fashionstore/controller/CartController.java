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
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.service.CartService;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerRepository customerRepository;

    // TODO: Thay thế hàm này bằng logic lấy customer thật từ Spring Security
    private Customer getCurrentCustomer() {
        // Giả sử customer có ID = 1 đã đăng nhập. 
        // Lấy customer THẬT từ database thay vì tạo object mới
        return customerRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String viewCart(Model model, HttpSession session) {
        Customer currentCustomer = getCurrentCustomer();
        List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
        double total = cartService.getCartTotal(cartItems);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", total);
        
        // Cập nhật số lượng giỏ hàng vào session
        session.setAttribute("cartCount", cartItems.size());
        
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam("sizeId") Integer sizeId,
                            @RequestParam("colorId") Integer colorId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer();
            // Tìm variant dựa trên productId, sizeId và colorId
            Integer variantId = cartService.findVariantByProductSizeColor(productId, sizeId, colorId);
            
            if (variantId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm với size và màu đã chọn!");
                return "redirect:/products/detail?id=" + productId;
            }
            
            cartService.addToCart(currentCustomer, variantId, quantity);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, currentCustomer);
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
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
            updateCartCount(session, getCurrentCustomer());
            
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
            updateCartCount(session, getCurrentCustomer());
            
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
}
