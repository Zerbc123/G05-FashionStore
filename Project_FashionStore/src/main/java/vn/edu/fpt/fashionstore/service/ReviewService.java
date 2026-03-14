package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.OrderStatus;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Review;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.repository.ReviewRepository;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    // 1. Kiểm tra xem khách hàng có được phép đánh giá không?
    public boolean canCustomerReviewProduct(Customer customer, Long productId) {
        if (customer == null || productId == null) return false;
        // Phải là đơn hàng đã giao thành công (COMPLETED)
        return orderRepository.hasCustomerBoughtProduct(customer, productId, OrderStatus.COMPLETED);
    }

    // 2. Lưu đánh giá của khách hàng vào Database
    public Review saveReview(Review review) {
        return reviewRepository.save(review);
    }

    // 3. Lấy danh sách đánh giá HIỂN THỊ cho khách hàng xem ở trang sản phẩm
    public List<Review> getActiveReviewsByProduct(Product product) {
        return reviewRepository.findByProductAndIsActiveTrueOrderByReviewDateDesc(product);
    }

    // 4. Lấy TẤT CẢ đánh giá cho ADMIN quản lý
    public List<Review> getAllReviewsForAdmin() {
        return reviewRepository.findAllByOrderByReviewDateDesc();
    }

    // 5. Tính năng cho Admin: Ẩn/Hiện đánh giá (nếu khách chửi bậy, admin có thể ẩn đi)
    public void toggleReviewStatus(Long reviewId) {
        reviewRepository.findById(reviewId).ifPresent(review -> {
            review.setIsActive(!review.getIsActive()); // Đang true thì thành false, đang false thành true
            reviewRepository.save(review);
        });
    }
}