package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.OrderStatus;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Review;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Lấy tất cả đánh giá của 1 sản phẩm để hiện lên trang chi tiết cho khách xem (chỉ lấy đánh giá chưa bị ẩn)
    List<Review> findByProductAndIsActiveTrueOrderByReviewDateDesc(Product product);

    // Dành cho Admin lấy tất cả đánh giá để quản lý (cũ nhất xuống dưới, mới nhất lên đầu)
    List<Review> findAllByOrderByReviewDateDesc();

    long countByCustomerAndProduct_ProductId(Customer customer, Long productId);

}