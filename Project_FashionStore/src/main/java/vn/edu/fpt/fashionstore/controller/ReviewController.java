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
        if (currentCustomer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để đánh giá!");
            return "redirect:/login";
        }

        // Kiểm tra bảo mật: Khách phải mua và nhận hàng (COMPLETED) rồi mới được đánh giá
        if (!reviewService.canCustomerReviewProduct(currentCustomer, productId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn phải mua và nhận hàng thành công mới được đánh giá sản phẩm này!");
            // ĐÃ SỬA ĐƯỜNG DẪN CHUẨN THEO PRODUCT CONTROLLER CỦA BẠN
            return "redirect:/products/detail/" + productId;
        }

        // Lưu đánh giá vào DB
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

        // ĐÃ SỬA ĐƯỜNG DẪN CHUẨN
        return "redirect:/products/detail/" + productId;
    }
}