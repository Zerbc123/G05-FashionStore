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
        
        Optional<Account> accountOpt = accountService.findByEmail(email);
        if (accountOpt.isEmpty() || accountOpt.get().getCustomers().isEmpty()) {
            return "redirect:/login";
        }
        
        Customer customer = accountOpt.get().getCustomers().get(0);
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
        
        Optional<Account> accountOpt = accountService.findByEmail(email);
        if (accountOpt.isEmpty() || accountOpt.get().getCustomers().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để thêm vào yêu thích");
            return "redirect:/login";
        }
        
        Customer customer = accountOpt.get().getCustomers().get(0);
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
        
        Optional<Account> accountOpt = accountService.findByEmail(email);
        if (accountOpt.isEmpty() || accountOpt.get().getCustomers().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin khách hàng");
            return "redirect:/wishlist";
        }
        
        Customer customer = accountOpt.get().getCustomers().get(0);
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
}
