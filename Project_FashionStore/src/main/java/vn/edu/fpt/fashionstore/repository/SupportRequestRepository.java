package vn.edu.fpt.fashionstore.repository;

import vn.edu.fpt.fashionstore.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.fashionstore.entity.SupportStatus;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    List<SupportRequest> findByAssignedStaffId(Integer staffId);

    Page<SupportRequest> findByStatus(SupportStatus status, Pageable pageable);

    Page<SupportRequest> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<SupportRequest> findByTitleContainingIgnoreCaseAndStatus(String keyword, SupportStatus status, Pageable pageable);

    List<SupportRequest> findByStatus(SupportStatus status);

    boolean existsByAssignedStaffId(Integer staffId);

}