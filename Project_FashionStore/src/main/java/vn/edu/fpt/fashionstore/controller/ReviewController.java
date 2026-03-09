package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Review;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.ProductRepository;
import vn.edu.fpt.fashionstore.repository.ReviewRepository;
import vn.edu.fpt.fashionstore.service.OrderService;
import vn.edu.fpt.fashionstore.service.ReviewService;

import java.util.Optional;

@Controller
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AccountRepository accountRepository;

    // ĐÃ THÊM: Nhúng 2 công cụ cần thiết vào để không báo đỏ nữa
    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewRepository reviewRepository;

    // Lấy thông tin khách hàng đang đăng nhập
    private Customer getCurrentCustomer(HttpSession session) {
        String email = (String) session.getAttribute("user");
        if (email == null) return null;
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty() || accountOpt.get().getCustomers().isEmpty()) return null;
        return accountOpt.get().getCustomers().get(0);
    }

    @PostMapping("/add")
    public String addReview(
            @RequestParam("productId") Long productId,
            @RequestParam("rating") Integer rating,
            @RequestParam("comment") String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Customer currentCustomer = getCurrentCustomer(session);

        // 1. KIỂM TRA ĐĂNG NHẬP ĐẦU TIÊN (Để tránh lỗi Null)
        if (currentCustomer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để đánh giá!");
            return "redirect:/login";
        }

        // 2. TÍNH TOÁN SỐ LẦN MUA VÀ SỐ LẦN ĐÃ ĐÁNH GIÁ
        long purchaseCount = orderService.countSuccessfulPurchases(currentCustomer, productId);
        long reviewCount = reviewRepository.countByCustomerAndProduct_ProductId(currentCustomer, productId);

        // 3. KIỂM TRA ĐIỀU KIỆN ĐÁNH GIÁ (Phải mua rồi và Số lần mua > Số lần đã đánh giá)
        if (purchaseCount == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải mua và nhận hàng thành công mới được đánh giá sản phẩm này!");
            return "redirect:/products/detail/" + productId;
        }

        if (purchaseCount <= reviewCount) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã hết lượt đánh giá cho lần mua này. Hãy mua thêm để tiếp tục đánh giá nhé!");
            return "redirect:/products/detail/" + productId;
        }

        // 4. LƯU ĐÁNH GIÁ VÀO DATABASE
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Review review = new Review();
            review.setCustomer(currentCustomer);
            review.setProduct(product);
            review.setRating(rating);
            review.setComment(comment);
            reviewService.saveReview(review);

            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã đánh giá sản phẩm!");
        }

        return "redirect:/products/detail/" + productId;
    }
}