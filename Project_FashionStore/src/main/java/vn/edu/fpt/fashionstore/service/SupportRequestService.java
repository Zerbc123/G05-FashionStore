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

    SupportRequest findById(Long id);

    void updateStatus(Long id, SupportStatus status);

    void deleteById(Long id);

    void create(SupportRequest request);

    boolean isStaffAssigned(Integer staffId);
}