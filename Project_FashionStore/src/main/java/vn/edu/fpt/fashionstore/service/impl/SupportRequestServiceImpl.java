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

        SupportRequest request = repository.findById(requestId).orElse(null);

        if (request != null) {

            Account staff = accountRepository.findById(staffId).orElse(null);

            request.setAssignedStaffId(staffId);

            if (staff != null) {
                request.setAssignedStaffName(staff.getFullName());
            }

            repository.save(request);
        }
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
    public void updateStatus(Long id, SupportStatus status) {
        SupportRequest request = repository.findById(id).orElse(null);
        if (request != null) {
            request.setStatus(status);
            repository.save(request);
        }
    }

    @Override
    public boolean isStaffAssigned(Integer staffId) {
        return repository.existsByAssignedStaffId(staffId);
    }
}