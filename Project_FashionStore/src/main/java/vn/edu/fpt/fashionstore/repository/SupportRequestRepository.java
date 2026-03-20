package vn.edu.fpt.fashionstore.repository;

import vn.edu.fpt.fashionstore.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.fashionstore.entity.SupportStatus;
import java.time.LocalDateTime;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    List<SupportRequest> findByAssignedStaffId(Integer staffId);

    Page<SupportRequest> findByStatus(SupportStatus status, Pageable pageable);

    Page<SupportRequest> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<SupportRequest> findByTitleContainingIgnoreCaseAndStatus(String keyword, SupportStatus status, Pageable pageable);

    List<SupportRequest> findByStatus(SupportStatus status);

    boolean existsByAssignedStaffId(Integer staffId);

    // Thêm các method tìm kiếm theo customer
    Page<SupportRequest> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);

    Page<SupportRequest> findByCustomerEmailContainingIgnoreCase(String customerEmail, Pageable pageable);

    Page<SupportRequest> findByCustomerNameContainingIgnoreCaseOrCustomerEmailContainingIgnoreCaseOrTitleContainingIgnoreCase(
            String customerName, String customerEmail, String title, Pageable pageable);

    Page<SupportRequest> findByCustomerNameContainingIgnoreCaseOrCustomerEmailContainingIgnoreCaseOrTitleContainingIgnoreCaseAndStatus(
            String customerName, String customerEmail, String title, SupportStatus status, Pageable pageable);

    // Thêm các method thống kê cho staff
    List<SupportRequest> findByAssignedStaffIdAndStatus(Integer staffId, SupportStatus status);
    
    List<SupportRequest> findByAssignedStaffIdAndCreatedAtBetween(Integer staffId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}