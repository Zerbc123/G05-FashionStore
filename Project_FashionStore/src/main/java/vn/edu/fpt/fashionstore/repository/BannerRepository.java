package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Banner;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Dành cho trang Admin: Lấy tất cả banner, sắp xếp theo thứ tự hiển thị
    List<Banner> findAllByOrderByDisplayOrderAsc();

    // Dành cho trang Chủ (Khách hàng): Chỉ lấy những banner ĐANG BẬT, sắp xếp đúng thứ tự
    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();
}