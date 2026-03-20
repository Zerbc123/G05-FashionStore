package vn.edu.fpt.fashionstore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.fpt.fashionstore.entity.SupportRequest;
import vn.edu.fpt.fashionstore.repository.SupportRequestRepository;
import vn.edu.fpt.fashionstore.service.SupportRequestService;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.fashionstore.entity.SupportStatus;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.entity.Account;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SupportRequestServiceImpl implements SupportRequestService {

    private final SupportRequestRepository repository;

    private final AccountRepository accountRepository;

    public SupportRequestServiceImpl(SupportRequestRepository repository,
                                     AccountRepository accountRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
    }

    @Override
    public List<SupportRequest> getAllRequests() {
        return repository.findAll();
    }


    @Override
    public void assignStaff(Long requestId, Integer staffId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }

        SupportRequest request = repository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Support request not found with ID: " + requestId));

        Account staff = accountRepository.findById(staffId)
            .orElseThrow(() -> new RuntimeException("Staff not found with ID: " + staffId));

        request.setAssignedStaffId(staffId);
        request.setAssignedStaffName(staff.getFullName());

        repository.save(request);
    }

    @Override
    public List<SupportRequest> getRequestsByStaff(Integer staffId) {
        return repository.findByAssignedStaffId(staffId);
    }

    @Override
    public Page<SupportRequest> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Page<SupportRequest> findByKeyword(String keyword, Pageable pageable) {
        return repository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public Page<SupportRequest> findByStatus(SupportStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable);
    }

    @Override
    public Page<SupportRequest> findByKeywordAndStatus(String keyword, SupportStatus status, Pageable pageable) {
        return repository.findByTitleContainingIgnoreCaseAndStatus(keyword, status, pageable);
    }

    @Override
    public SupportRequest findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void create(SupportRequest request) {
        repository.save(request);
    }

    @Override
    public SupportRequest save(SupportRequest request) {
        return repository.save(request);
    }

    @Override
    public void updateStatus(Long id, SupportStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        SupportRequest request = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Support request not found with ID: " + id));
        
        request.setStatus(status);
        repository.save(request);
    }

    @Override
    public Page<SupportRequest> findByCustomerKeyword(String keyword, Pageable pageable) {
        return repository.findByCustomerNameContainingIgnoreCaseOrCustomerEmailContainingIgnoreCaseOrTitleContainingIgnoreCase(
                keyword, keyword, keyword, pageable);
    }

    @Override
    public Page<SupportRequest> findByCustomerKeywordAndStatus(String keyword, SupportStatus status, Pageable pageable) {
        return repository.findByCustomerNameContainingIgnoreCaseOrCustomerEmailContainingIgnoreCaseOrTitleContainingIgnoreCaseAndStatus(
                keyword, keyword, keyword, status, pageable);
    }

    @Override
    public boolean isStaffAssigned(Integer staffId) {
        return repository.existsByAssignedStaffId(staffId);
    }

    // Implement các method thống kê cho staff
    @Override
    public List<SupportRequest> getRequestsByStaffAndStatus(Integer staffId, SupportStatus status) {
        return repository.findByAssignedStaffIdAndStatus(staffId, status);
    }
    
    @Override
    public List<SupportRequest> getRequestsByStaffAndDate(Integer staffId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return repository.findByAssignedStaffIdAndCreatedAtBetween(staffId, startOfDay, endOfDay);
    }
    
    @Override
    public long getAverageResponseTimeForStaff(Integer staffId) {
        List<SupportRequest> resolvedRequests = repository.findByAssignedStaffIdAndStatus(staffId, SupportStatus.RESOLVED);
        
        if (resolvedRequests.isEmpty()) {
            return 0;
        }
        
        long totalMinutes = 0;
        for (SupportRequest request : resolvedRequests) {
            if (request.getCreatedAt() != null && request.getUpdatedAt() != null) {
                long minutes = ChronoUnit.MINUTES.between(request.getCreatedAt(), request.getUpdatedAt());
                totalMinutes += minutes;
            }
        }
        
        return totalMinutes / resolvedRequests.size();
    }

    @Override
    public List<SupportRequest> findByCustomerEmail(String customerEmail) {
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            return List.of();
        }
        return repository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail);
    }
}