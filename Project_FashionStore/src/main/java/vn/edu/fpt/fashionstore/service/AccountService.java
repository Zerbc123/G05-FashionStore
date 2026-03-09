package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Role;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.RoleRepository;
import vn.edu.fpt.fashionstore.repository.SupportRequestRepository;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    public AccountService(AccountRepository accountRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Lấy tất cả nhân viên trừ những nhân viên đã bị xóa(có phân trang)
    public Page<Account> getAllStaff(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());

        return accountRepository.findByStatusIn(
                List.of("ACTIVE", "LOCKED"),
                pageable
        );
    }

    // Tìm kiếm nhân viên (có phân trang)
    public Page<Account> searchStaff(String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStaff(page, size);
        }

        return accountRepository.searchNotDeleted(
                keyword.trim(),
                pageable
        );
    }

    // Lấy tất cả nhân viên (không phân trang - giữ lại để dùng ở chỗ khác)
    public List<Account> getAllStaff() {
        return accountRepository.findAll();
    }

    // Lấy role theo ID
    public Role getRoleById(Integer roleId) {
        return roleRepository.findById(roleId).orElse(null);
    }

    // Tạo STAFF mới (FIX ROLE NULL + TRÙNG + MÃ HÓA MẬT KHẨU)
    public void createStaff(Account account, Integer roleId) {

        if (accountRepository.existsByUsername(account.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        Role role = null;
        if (roleId != null) {
            role = roleRepository.findById(roleId).orElse(null);
        }
        if (role == null) {
            role = roleRepository.findByRoleName("Nhân viên bán hàng (Sale)");
        }
        if (role == null) {
            throw new RuntimeException("Role 'Nhân viên bán hàng (Sale)' không tồn tại trong DB");
        }

        // MÃ HÓA MẬT KHẨU trước khi lưu
        String rawPassword = account.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        account.setPassword(encodedPassword);
        
        System.out.println("🔐 Password encoding:");
        System.out.println("   Original: " + rawPassword);
        System.out.println("   Encoded:  " + encodedPassword);

        account.setRole(role);
        account.setStatus("ACTIVE"); // Set default status

        accountRepository.save(account);

        // GỬI EMAIL SAU KHI SAVE
        emailService.sendEmail(
                account.getEmail(),
                account.getFullName(),
                account.getUsername(),
                rawPassword,
                role.getRoleName()
        );
    }

    // Khóa
    public void lockAccount(Integer accountId) {
        Account acc = getById(accountId);
        acc.setStatus("LOCKED");
        accountRepository.save(acc);
    }

    // Mở khóa
    public void unlockAccount(Integer accountId) {
        Account acc = getById(accountId);
        acc.setStatus("ACTIVE");
        accountRepository.save(acc);
    }

    // Lấy theo ID
    public Account getById(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
    }

    // Update staff
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

    // Lấy tất cả roles (để hiển thị trong form tạo mới)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    // Xóa tài khoản/xóa mềm (gỡ nhân viên)
    public void deleteAccount(Integer id) {

        Account account = getById(id);

        account.setStatus("INACTIVE");

        accountRepository.save(account);
    }

    // XÓA CỨNG (CHỈ XÓA TÀI KHOẢN INACTIVE)
    public void hardDeleteAccount(Integer id) {

        Account account = getById(id);

        if (!"INACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Chỉ được xóa vĩnh viễn tài khoản đã INACTIVE");
        }

        try {
            // Kiểm tra xem có dữ liệu liên quan không (nếu có bảng liên quan)
            // Note: Method này sẽ hoạt động khi có các bảng khác tham chiếu đến Account
            // Long relatedCount = accountRepository.countRelatedData(id);
            // if (relatedCount != null && relatedCount > 0) {
            //     throw new RuntimeException("Không thể xóa nhân viên này vì vẫn còn " + relatedCount + " bản ghi liên quan trong hệ thống. Vui lòng kiểm tra lại các bản ghi liên quan trước khi xóa.");
            // }
            
            accountRepository.delete(account);
        } catch (Exception ex) {
            // Kiểm tra xem có phải là lỗi foreign key constraint không
            String errorMessage = ex.getMessage();
            if (errorMessage != null && (errorMessage.contains("foreign key constraint") || 
                                     errorMessage.contains("violates foreign key constraint") ||
                                     errorMessage.contains("Cannot delete or update a parent row"))) {
                throw new RuntimeException("Không thể xóa nhân viên này vì vẫn còn dữ liệu liên quan trong hệ thống. Vui lòng kiểm tra lại các bản ghi liên quan trước khi xóa.");
            } else if (errorMessage != null && errorMessage.contains("constraint")) {
                throw new RuntimeException("Không thể xóa nhân viên này vì vi phạm ràng buộc dữ liệu. Vui lòng kiểm tra lại các dữ liệu liên quan.");
            } else {
                throw new RuntimeException("Lỗi khi xóa nhân viên: " + ex.getMessage());
            }
        }
    }

    // LẤY TẤT CẢ NHÂN VIÊN ĐÃ BỊ XÓA (INACTIVE) - PHÂN TRANG
    public Page<Account> getDeletedStaff(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        return accountRepository.findByStatus("INACTIVE", pageable);
    }

    // TÌM KIẾM NHÂN VIÊN ĐÃ BỊ XÓA (INACTIVE) - PHÂN TRANG
    public Page<Account> searchDeletedStaff(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());

        if (keyword == null || keyword.trim().isEmpty()) {
            return getDeletedStaff(page, size);
        }

        return accountRepository.searchDeleted(keyword.trim(), pageable);
    }

    // KHÔI PHỤC TÀI KHOẢN NHÂN VIÊN
    public void restoreAccount(Integer id) {
        Account account = getById(id);
        
        if (!"INACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Chỉ có thể khôi phục tài khoản đã bị xóa (INACTIVE)");
        }

        account.setStatus("ACTIVE");
        accountRepository.save(account);
    }

    //Lọc những tài khoản SUPPORT
    public List<Account> getSupportStaff() {
        return accountRepository.findByRole_RoleName("SUPPORT");
    }

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    public boolean isStaffAssigned(Integer staffId) {
        return supportRequestRepository.existsByAssignedStaffId(staffId);
    }

}
