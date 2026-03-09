
package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Voucher;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // Hàm kiểm tra xem mã Code đã bị Admin khác tạo trùng chưa
    boolean existsByCode(String code);

    // Hàm tìm Voucher dựa vào chuỗi Code (Sẽ dùng rất nhiều ở bước Apply Voucher sau này)
    Optional<Voucher> findByCode(String code);

    // Lấy các mã đang kích hoạt (isActive = true) và ngày hết hạn >= ngày hôm nay
    List<Voucher> findByIsActiveTrueAndExpiredDateGreaterThanEqual(java.util.Date date);
}
