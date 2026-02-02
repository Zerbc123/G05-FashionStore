package vn.edu.fpt.fashionstore.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Role;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.RoleRepository;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    public AccountService(AccountRepository accountRepository,
                          RoleRepository roleRepository) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
    }

    // 1. Lấy tất cả nhân viên (có phân trang)
    public Page<Account> getAllStaff(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        return accountRepository.findAll(pageable);
    }

    // 2. Tìm kiếm nhân viên (có phân trang)
    public Page<Account> searchStaff(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        if (keyword == null || keyword.trim().isEmpty()) {
            return accountRepository.findAll(pageable);
        }
        return accountRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
            keyword.trim(), keyword.trim(), keyword.trim(), pageable);
    }

    // 3. Lấy tất cả nhân viên (không phân trang - giữ lại để dùng ở chỗ khác)
    public List<Account> getAllStaff() {
        return accountRepository.findAll();
    }

    // 2. Tạo STAFF mới (FIX ROLE NULL + TRÙNG)
    public void createStaff(Account account, Integer roleId) {

        if (accountRepository.existsByUsername(account.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        Role role = null;
        if (roleId != null && roleId > 0) {
            role = roleRepository.findById(roleId).orElse(null);
        }
        if (role == null) {
            role = roleRepository.findByRoleName("Nhân viên bán hàng (Sale)");
        }
        if (role == null) {
            throw new RuntimeException("Role 'Nhân viên bán hàng (Sale)' không tồn tại trong DB");
        }

        account.setRole(role);
        account.setStatus("ACTIVE");

        accountRepository.save(account);
    }

    // 3. Khóa
    public void lockAccount(Integer accountId) {
        Account acc = getById(accountId);
        acc.setStatus("LOCKED");
        accountRepository.save(acc);
    }

    // 4. Mở khóa
    public void unlockAccount(Integer accountId) {
        Account acc = getById(accountId);
        acc.setStatus("ACTIVE");
        accountRepository.save(acc);
    }

    // 5. Lấy theo ID
    public Account getById(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
    }

    // 6. Update staff
    public void updateStaff(Account staff) {
        Account existing = getById(staff.getAccountId());

        existing.setFullName(staff.getFullName());
        existing.setEmail(staff.getEmail());
        existing.setPhone(staff.getPhone());
        existing.setStatus(staff.getStatus());
        
        // Cập nhật role nếu có thay đổi
        if (staff.getRole() != null && staff.getRole().getRoleId() != null) {
            Role newRole = roleRepository.findById(staff.getRole().getRoleId()).orElse(null);
            if (newRole != null) {
                existing.setRole(newRole);
            }
        }

        accountRepository.save(existing);
    }

    // 7. Lấy tất cả roles (để hiển thị trong form tạo mới)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // 8. Xóa tài khoản (gỡ nhân viên)
    public void deleteAccount(Integer id) {
        accountRepository.deleteById(id);
    }
}
