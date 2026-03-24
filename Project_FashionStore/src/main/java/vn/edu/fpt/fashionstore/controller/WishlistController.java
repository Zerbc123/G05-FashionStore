package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Wishlist;
import vn.edu.fpt.fashionstore.repository.ProductRepository;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.WishlistService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/wishlist")
public class WishlistController {
    
    @Autowired
    private WishlistService wishlistService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping
    public String viewWishlist(HttpSession session, Model model) {
        String email = (String) session.getAttribute("user");
        if (email == null) {
            return "redirect:/login";
        }
        
        Customer customer = accountService.findCustomerByEmail(email);
        if (customer == null) {
            return "redirect:/login";
        }
        
        List<Wishlist> wishlistItems = wishlistService.getWishlistByCustomer(customer);
        
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("wishlistCount", wishlistItems.size());
        
        return "wishlist";
    }
    
    @PostMapping("/add")
    public String addToWishlist(@RequestParam Long productId, HttpSession session, 
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("user");
        if (email == null) {
            return "redirect:/login";
        }
        
        Customer customer = accountService.findCustomerByEmail(email);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin khách hàng");
            return "redirect:/login";
        }
        
        Optional<Product> productOpt = productRepository.findById(productId);
        
        boolean added = wishlistService.addToWishlist(customer, productOpt.get());
        
        if (added) {
            redirectAttributes.addFlashAttribute("success", "Đã thêm vào danh sách yêu thích");
        } else {
            redirectAttributes.addFlashAttribute("error", "Sản phẩm đã có trong danh sách yêu thích");
        }
        
        return "redirect:/products";
    }
    
    @PostMapping("/remove")
    public String removeFromWishlist(@RequestParam Long productId, HttpSession session,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("user");
        if (email == null) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để xóa khỏi yêu thích");
            return "redirect:/login";
        }
        
        Customer customer = accountService.findCustomerByEmail(email);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin khách hàng");
            return "redirect:/wishlist";
        }
        
        Optional<Product> productOpt = productRepository.findById(productId);
        
        if (productOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm");
            return "redirect:/wishlist";
        }
        
        boolean removed = wishlistService.removeFromWishlist(customer, productOpt.get());
        
        if (removed) {
            redirectAttributes.addFlashAttribute("success", "Đã xóa khỏi danh sách yêu thích");
        } else {
            redirectAttributes.addFlashAttribute("error", "Sản phẩm không có trong danh sách yêu thích");
        }
        
        return "redirect:/wishlist";
    }

    @PostMapping("/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(@RequestParam Long productId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String email = (String) session.getAttribute("user");
        
        if (email == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để thực hiện hành động này");
            return ResponseEntity.status(401).body(response);
        }
        
        Customer customer = accountService.findCustomerByEmail(email);
        if (customer == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy thông tin khách hàng");
            return ResponseEntity.status(404).body(response);
        }
        
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Sản phẩm không tồn tại");
            return ResponseEntity.status(404).body(response);
        }
        
        Product product = productOpt.get();
        boolean isInWishlist = wishlistService.isInWishlist(customer, product);
        
        if (isInWishlist) {
            wishlistService.removeFromWishlist(customer, product);
            response.put("status", "removed");
            response.put("message", "Đã xóa khỏi danh sách yêu thích");
        } else {
            wishlistService.addToWishlist(customer, product);
            response.put("status", "added");
            response.put("message", "Đã thêm vào danh sách yêu thích");
        }
        
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
