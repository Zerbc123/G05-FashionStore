package vn.edu.fpt.fashionstore.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Role;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.repository.RoleRepository;
import vn.edu.fpt.fashionstore.repository.SupportRequestRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public static final String ROLE_CUSTOMER = "Customer";

    /* =================================================
                    ADMIN - STAFF MANAGEMENT
       ================================================= */

    public Page<Account> getAllStaff(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        return accountRepository.findStaffAccounts(pageable);
    }

    public Page<Account> searchStaff(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStaff(page, size);
        }
        return accountRepository.searchStaffAccounts(keyword.trim(), pageable);
    }

    public Role getRoleById(Integer roleId) {
        return roleRepository.findById(roleId).orElse(null);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<Role> getStaffRoles() {
        List<Role> roles = roleRepository.findAll();
        // Loại bỏ Admin và Customer
        roles.removeIf(role ->
                role.getRoleName().equalsIgnoreCase("Admin") ||
                role.getRoleName().equalsIgnoreCase("Customer")
        );
        return roles;
    }

    public void createStaff(Account account, Integer roleId) {
        if (accountRepository.existsByUsername(account.getUsername()))
            throw new RuntimeException("Username đã tồn tại");
        if (accountRepository.existsByEmail(account.getEmail()))
            throw new RuntimeException("Email đã tồn tại");

        Role role = roleRepository.findById(roleId)
                .orElseGet(() -> roleRepository.findByRoleName("SUPPORT").orElse(null));

        if (role == null)
            throw new RuntimeException("Role không tồn tại");

        String rawPassword = account.getPassword();
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setStatus("ACTIVE");

        accountRepository.save(account);

        emailService.sendEmail(
                account.getEmail(),
                account.getFullName(),
                account.getUsername(),
                rawPassword,
                role.getRoleName()
        );
    }

    public void lockAccount(Integer id) {
        Account acc = getById(id);
        acc.setStatus("LOCKED");
        accountRepository.save(acc);
    }

    public void unlockAccount(Integer id) {
        Account acc = getById(id);
        acc.setStatus("ACTIVE");
        accountRepository.save(acc);
    }

    public Account getById(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }

    public void deleteAccount(Integer id) {
        Account account = getById(id);
        account.setStatus("INACTIVE");
        accountRepository.save(account);
    }

    public void restoreAccount(Integer id) {
        Account account = getById(id);
        account.setStatus("ACTIVE");
        accountRepository.save(account);
    }

    public void updateStaff(Account account) {
        if (account == null || account.getAccountId() == null) {
            throw new RuntimeException("Account không hợp lệ");
        }
        Account existingAccount = getById(account.getAccountId());
        if (accountRepository.existsByUsernameAndAccountIdNot(account.getUsername(), account.getAccountId())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (accountRepository.existsByEmailAndAccountIdNot(account.getEmail(), account.getAccountId())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        existingAccount.setUsername(account.getUsername());
        existingAccount.setEmail(account.getEmail());
        existingAccount.setFullName(account.getFullName());
        existingAccount.setPhone(account.getPhone());
        if (account.getRole() != null && account.getRole().getRoleId() != null) {
            Role role = roleRepository.findById(account.getRole().getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            existingAccount.setRole(role);
        }
        if (account.getPassword() != null && !account.getPassword().trim().isEmpty()) {
            existingAccount.setPassword(passwordEncoder.encode(account.getPassword()));
        }
        accountRepository.save(existingAccount);
    }

    public List<Account> getSupportStaff() {
        return accountRepository.findByRole_RoleName("SUPPORT");
    }

    // Test method để kiểm tra việc lấy Account từ DB
    public String testAccountConnection(){
        try {
            long totalAccounts = accountRepository.count();
            Optional<Account> adminAccount = accountRepository.findByUsername("admin");
            Optional<Account> firstAccount = accountRepository.findById(1);
            
            StringBuilder result = new StringBuilder();
            result.append("=== Account Connection Test ===\n");
            result.append("Total accounts: ").append(totalAccounts).append("\n");
            
            if(adminAccount.isPresent()){
                Account admin = adminAccount.get();
                result.append("Admin found: ").append(admin.getUsername()).append(" (").append(admin.getEmail()).append(")\n");
            } else {
                result.append("Admin NOT found\n");
            }
            
            if(firstAccount.isPresent()){
                Account first = firstAccount.get();
                result.append("First account: ").append(first.getUsername()).append(" (ID: ").append(first.getAccountId()).append(")\n");
            } else {
                result.append("No account with ID=1 found\n");
            }
            
            // Test role mapping
            List<Role> roles = getAllRoles();
            result.append("Total roles: ").append(roles.size()).append("\n");
            for(Role role : roles){
                result.append("- ").append(role.getRoleName()).append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public boolean isStaffAssigned(Integer staffId) {
        return supportRequestRepository.existsByAssignedStaffId(staffId);
    }

    // Các method cho trash functionality
    public Page<Account> getDeletedStaff(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        return accountRepository.findByStatus("INACTIVE", pageable);
    }

    public Page<Account> searchDeletedStaff(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        if (keyword == null || keyword.trim().isEmpty()) {
            return getDeletedStaff(page, size);
        }
        return accountRepository.searchDeleted(keyword.trim(), pageable);
    }

    public void hardDeleteAccount(Integer id) {
        Account account = getById(id);
        accountRepository.delete(account);
    }

    /* =================================================
                        LOGIN / REGISTER
       ================================================= */

    public Account authenticate(String username, String password) {
        Account account = accountRepository.findByUsername(username)
                .orElseGet(() -> accountRepository.findByEmail(username).orElse(null));
        if (account == null || account.getPassword() == null) return null;
        boolean passwordValid;
        if (account.getPassword().startsWith("$2a$")) {
            passwordValid = passwordEncoder.matches(password, account.getPassword());
        } else {
            passwordValid = account.getPassword().equals(password);
        }
        if (!passwordValid || !"ACTIVE".equalsIgnoreCase(account.getStatus())) return null;
        return account;
    }

    @Transactional
    public Account registerAccount(String email, String password, String fullName, String phone) {
        if (accountRepository.existsByEmail(email)) return null;

        String baseUsername = email.split("@")[0];
        String finalUsername = baseUsername;
        int count = 1;
        while (accountRepository.existsByUsername(finalUsername)) {
            finalUsername = baseUsername + count++;
        }

        Account account = new Account();
        account.setUsername(finalUsername);
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setFullName(fullName);
        account.setPhone(phone);
        account.setStatus("ACTIVE");

        Role role = roleRepository.findByRoleName("Customer")
                .orElseGet(() -> roleRepository.findById(3).orElse(null));
        account.setRole(role);

        Account saved = accountRepository.save(account);

        Customer customer = new Customer();
        customer.setAccount(saved);
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setCreatedDate(LocalDate.now());
        customerRepository.save(customer);

        return saved;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account acc = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        String roleName = (acc.getRole() != null) ? acc.getRole().getRoleName() : "USER";
        return org.springframework.security.core.userdetails.User
                .withUsername(acc.getEmail())
                .password(acc.getPassword())
                .roles(roleName)
                .build();
    }

    /* =================================================
                        PROFILE MANAGEMENT
       ================================================= */

    @Transactional
    public Account updateProfile(String email, String fullName, String phone, String address, String gender, LocalDate dateOfBirth) {
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) return null;

        Account account = accountOpt.get();
        account.setFullName(fullName);
        if (phone != null && !phone.isEmpty()) {
            account.setPhone(phone.replaceAll("[^0-9]", ""));
        }

        Customer customer;
        if (account.getCustomers() != null && !account.getCustomers().isEmpty()) {
            customer = account.getCustomers().get(0);
        } else {
            customer = new Customer();
            customer.setAccount(account);
            customer.setCreatedDate(LocalDate.now());
            customer.setEmail(email); // Ensure email is set for new customer
        }

        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setAddress(address);
        if (gender != null) {
            customer.setGender("Nam".equalsIgnoreCase(gender)); // Assuming "Nam" is true, "Nữ" is false
        }
        customer.setDateOfBirth(dateOfBirth);
        
        customerRepository.save(customer);
        return accountRepository.save(account);
    }

    @Transactional
    public boolean changePassword(String email, String currentPassword, String newPassword) {
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) return false;
        Account account = accountOpt.get();
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) return false;
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
        return true;
    }

    public Account getAccountByEmail(String email) {
        return accountRepository.findByEmail(email).orElse(null);
    }

    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }

    /* =================================================
                        OAUTH LOGIN
       ================================================= */

    @Transactional
    public Account processOAuthPostLogin(String email, String fullName) {
        Optional<Account> existAccount = accountRepository.findByEmail(email);
        if (existAccount.isEmpty()) {
            String baseUsername = email.split("@")[0];
            String finalUsername = baseUsername;
            int count = 1;
            while (accountRepository.existsByUsername(finalUsername)) {
                finalUsername = baseUsername + count++;
            }
            Account newAccount = new Account();
            newAccount.setUsername(finalUsername);
            newAccount.setEmail(email);
            newAccount.setFullName(fullName);
            newAccount.setPassword(null); // No password for Google users
            newAccount.setStatus("ACTIVE");
            Role customerRole = roleRepository.findByRoleName("Customer")
                    .orElseGet(() -> roleRepository.findById(3).orElse(null));
            newAccount.setRole(customerRole);
            Account savedAccount = accountRepository.save(newAccount);
            
            Customer customer = new Customer();
            customer.setAccount(savedAccount);
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setCreatedDate(LocalDate.now());
            customerRepository.save(customer);
            return savedAccount;
        }

        Account account = existAccount.get();
        // If account exists but customer record is missing, create it
        if (account.getCustomers() == null || account.getCustomers().isEmpty()) {
            Customer customer = new Customer();
            customer.setAccount(account);
            customer.setFullName(fullName != null ? fullName : account.getFullName());
            customer.setEmail(email);
            customer.setCreatedDate(LocalDate.now());
            customerRepository.save(customer);
        }
        return account;
    }
}
