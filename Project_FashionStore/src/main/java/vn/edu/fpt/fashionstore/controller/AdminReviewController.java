package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Review;
import vn.edu.fpt.fashionstore.service.ReviewService;

import java.util.List;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    @Autowired
    private ReviewService reviewService;

    // Hàm kiểm tra xem có đúng là Admin đang đăng nhập không
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role);
    }

    // 1. Mở trang Quản lý đánh giá
    @GetMapping
    public String manageReviews(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login"; // Không phải admin thì đuổi ra chuồng gà
        }

        // Lấy tất cả đánh giá ra (Bao gồm cả cái đang ẩn và đang hiện)
        List<Review> reviews = reviewService.getAllReviewsForAdmin();
        model.addAttribute("reviews", reviews);

        return "admin/managereviews"; // Trỏ tới file HTML của admin
    }

    // 2. Xử lý khi Admin bấm nút Ẩn/Hiện
    @PostMapping("/toggle/{id}")
    public String toggleReviewStatus(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Gọi service để đảo ngược trạng thái (đang Hiện -> Ẩn, đang Ẩn -> Hiện)
        reviewService.toggleReviewStatus(id);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đánh giá thành công!");
        return "redirect:/admin/reviews";
    }
}