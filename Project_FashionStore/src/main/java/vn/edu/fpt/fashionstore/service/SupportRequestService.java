package vn.edu.fpt.fashionstore.service;

import vn.edu.fpt.fashionstore.entity.SupportRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.fashionstore.entity.SupportStatus;

public interface SupportRequestService {

    List<SupportRequest> getAllRequests();

    void assignStaff(Long requestId, Integer staffId);

    List<SupportRequest> getRequestsByStaff(Integer staffId);

    Page<SupportRequest> findAll(Pageable pageable);

    Page<SupportRequest> findByKeyword(String keyword, Pageable pageable);

    Page<SupportRequest> findByStatus(SupportStatus status, Pageable pageable);

    Page<SupportRequest> findByKeywordAndStatus(String keyword, SupportStatus status, Pageable pageable);

    // Thêm method tìm kiếm mở rộng
    Page<SupportRequest> findByCustomerKeyword(String keyword, Pageable pageable);

    Page<SupportRequest> findByCustomerKeywordAndStatus(String keyword, SupportStatus status, Pageable pageable);

    SupportRequest findById(Long id);

    void updateStatus(Long id, SupportStatus status);

    void deleteById(Long id);

    void create(SupportRequest request);

    SupportRequest save(SupportRequest request);

    List<SupportRequest> findByCustomerEmail(String customerEmail);

    boolean isStaffAssigned(Integer staffId);

    // Thống kê cho staff
    List<SupportRequest> getRequestsByStaffAndStatus(Integer staffId, SupportStatus status);
    
    List<SupportRequest> getRequestsByStaffAndDate(Integer staffId, java.time.LocalDate date);
    
    long getAverageResponseTimeForStaff(Integer staffId);
}