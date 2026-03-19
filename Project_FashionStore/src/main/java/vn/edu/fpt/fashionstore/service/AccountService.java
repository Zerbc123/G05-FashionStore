package vn.edu.fpt.fashionstore.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Role;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.repository.RoleRepository;
import vn.edu.fpt.fashionstore.repository.SupportRequestRepository;

import java.util.*;
import java.sql.Date;

@Service
public class AccountService {

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

    public Page<Account> searchStaff(String keyword,int page,int size){
        Pageable pageable = PageRequest.of(page,size,Sort.by("accountId").descending());

        if(keyword == null || keyword.trim().isEmpty()){
            return getAllStaff(page,size);
        }

        return accountRepository.searchStaffAccounts(keyword.trim(), pageable);
    }

    public Role getRoleById(Integer roleId){
        return roleRepository.findById(roleId).orElse(null);
    }

    public List<Role> getAllRoles(){
        return roleRepository.findAll();
    }

    public List<Role> getStaffRoles(){
        List<Role> roles = roleRepository.findAll();

        // loại bỏ Admin và Customer
        roles.removeIf(role ->
                role.getRoleName().equalsIgnoreCase("Admin") ||
                        role.getRoleName().equalsIgnoreCase("Customer")
        );

        return roles;
    }

    public boolean existsByPhone(String phone) {
        return accountRepository.existsByPhone(phone);
    }

    public boolean existsByPhoneAndAccountIdNot(String phone, Integer accountId) {
        return accountRepository.existsByPhoneAndAccountIdNot(phone, accountId);
    }

    public void createStaff(Account account,Integer roleId){

        if(accountRepository.existsByUsername(account.getUsername()))
            throw new RuntimeException("Username đã tồn tại");

        if(accountRepository.existsByEmail(account.getEmail()))
            throw new RuntimeException("Email đã tồn tại");

        if(account.getPhone() != null && !account.getPhone().trim().isEmpty() 
           && accountRepository.existsByPhone(account.getPhone()))
            throw new RuntimeException("Số điện thoại đã tồn tại");

        Role role = roleRepository.findById(roleId)
                .orElseGet(() -> roleRepository.findByRoleName("SUPPORT").orElse(null));

        if(role == null)
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

    public void lockAccount(Integer id){
        Account acc = getById(id);
        acc.setStatus("LOCKED");
        accountRepository.save(acc);
    }

    public void unlockAccount(Integer id){
        Account acc = getById(id);
        acc.setStatus("ACTIVE");
        accountRepository.save(acc);
    }

    public void leaveAccount(Integer id){
        Account acc = getById(id);
        acc.setStatus("LEAVE");
        accountRepository.save(acc);
    }

    public Account getById(Integer id){
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }

    public void deleteAccount(Integer id){
        Account account = getById(id);
        account.setStatus("INACTIVE");
        accountRepository.save(account);
    }

    public void restoreAccount(Integer id){
        Account account = getById(id);
        account.setStatus("ACTIVE");
        accountRepository.save(account);
    }

    public void updateStaff(Account account){
        if(account == null || account.getAccountId() == null){
            throw new RuntimeException("Account không hợp lệ");
        }
        
        // Kiểm tra xem account có tồn tại không
        Account existingAccount = getById(account.getAccountId());
        
        // Kiểm tra username và email có bị trùng không (trừ với chính nó)
        if(accountRepository.existsByUsernameAndAccountIdNot(account.getUsername(), account.getAccountId())){
            throw new RuntimeException("Username đã tồn tại");
        }
        
        if(accountRepository.existsByEmailAndAccountIdNot(account.getEmail(), account.getAccountId())){
            throw new RuntimeException("Email đã tồn tại");
        }
        
        // Kiểm tra số điện thoại có bị trùng không (trừ với chính nó)
        if(account.getPhone() != null && !account.getPhone().trim().isEmpty() 
           && accountRepository.existsByPhoneAndAccountIdNot(account.getPhone(), account.getAccountId())){
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        
        // Cập nhật thông tin
        existingAccount.setUsername(account.getUsername());
        existingAccount.setEmail(account.getEmail());
        existingAccount.setFullName(account.getFullName());
        existingAccount.setPhone(account.getPhone());
        
        // Cập nhật status nếu có
        if (account.getStatus() != null && !account.getStatus().trim().isEmpty()) {
            existingAccount.setStatus(account.getStatus());
        }
        
        // Cập nhật role nếu có
        if(account.getRole() != null && account.getRole().getRoleId() != null){
            Role role = roleRepository.findById(account.getRole().getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            existingAccount.setRole(role);
        }
        
        // Cập nhật password nếu có và không rỗng
        if(account.getPassword() != null && !account.getPassword().trim().isEmpty()){
            existingAccount.setPassword(passwordEncoder.encode(account.getPassword()));
        }
        
        accountRepository.save(existingAccount);
    }

    public List<Account> getSupportStaff(){
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

    public boolean isStaffAssigned(Integer staffId){
        return supportRequestRepository.existsByAssignedStaffId(staffId);
    }

    // Các method cho trash functionality
    public Page<Account> getDeletedStaff(int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        return accountRepository.findByStatus("INACTIVE", pageable);
    }

    public Page<Account> searchDeletedStaff(String keyword, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        if(keyword == null || keyword.trim().isEmpty()){
            return getDeletedStaff(page, size);
        }
        return accountRepository.searchDeleted(keyword.trim(), pageable);
    }

    public void hardDeleteAccount(Integer id){
        Account account = getById(id);
        accountRepository.delete(account);
    }

    /* =================================================
                        LOGIN
       ================================================= */

    public Account authenticate(String username,String password){

        Account account = accountRepository.findByUsername(username)
                .orElseGet(() -> accountRepository.findByEmail(username).orElse(null));

        if(account == null) return null;

        if(account.getPassword() == null) return null;

        boolean passwordValid;

        if(account.getPassword().startsWith("$2a$")){
            passwordValid = passwordEncoder.matches(password,account.getPassword());
        }else{
            passwordValid = account.getPassword().equals(password);
        }

        if(!passwordValid) return null;

        if(!"ACTIVE".equalsIgnoreCase(account.getStatus())) return null;

        return account;
    }

    //REGISTER

    public Account registerAccount(String email,String password,String fullName,String phone){

        if(accountRepository.existsByEmail(email))
            return null;

        String baseUsername = email.split("@")[0];
        String finalUsername = baseUsername;
        int count = 1;

        while(accountRepository.existsByUsername(finalUsername)){
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
        customer.setCreatedDate(new java.sql.Date(System.currentTimeMillis()));

        customerRepository.save(customer);

        return saved;
    }

    //PROFILE

    @Transactional
    public Account updateProfile(String email,
                                 String fullName,
                                 String phone,
                                 String address,
                                 String gender,
                                 java.util.Date dateOfBirth){

        Optional<Account> accountOpt = accountRepository.findByEmail(email);

        if(accountOpt.isEmpty()) return null;

        Account account = accountOpt.get();

        account.setFullName(fullName);
        account.setPhone(phone);

        // cập nhật customer info
        if(account.getCustomers() != null && !account.getCustomers().isEmpty()){

            Customer customer = account.getCustomers().get(0);

            customer.setFullName(fullName);
            customer.setPhone(phone);
            customer.setAddress(address);
            customer.setGender("male".equalsIgnoreCase(gender));
            
            // Convert java.util.Date to java.sql.Date if dateOfBirth is not null
            if(dateOfBirth != null){
                customer.setDateOfBirth(new java.sql.Date(dateOfBirth.getTime()));
            }

            customerRepository.save(customer);
        }

        return accountRepository.save(account);
    }

    //CHANGE PASSWORD

    @Transactional
    public boolean changePassword(String email,String currentPassword,String newPassword){

        Optional<Account> accountOpt = accountRepository.findByEmail(email);

        if(accountOpt.isEmpty()) return false;

        Account account = accountOpt.get();

        if(!passwordEncoder.matches(currentPassword,account.getPassword()))
            return false;

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        return true;
    }

    public Account getAccountByEmail(String email){
        return accountRepository.findByEmail(email).orElse(null);
    }

/* =================================================
                FIND ACCOUNT
   ================================================= */

    public Optional<Account> findByEmail(String email){
        return accountRepository.findByEmail(email);
    }

    public Account saveAccount(Account account){
        return accountRepository.save(account);
    }

/* =================================================
                GOOGLE OAUTH LOGIN
   ================================================= */

    public void processOAuthPostLogin(String email, String name) {

        Optional<Account> accountOpt = accountRepository.findByEmail(email);

        // Nếu account chưa tồn tại thì tạo mới
        if (accountOpt.isEmpty()) {

            Account account = new Account();

            account.setEmail(email);
            account.setFullName(name);
            account.setUsername(email.split("@")[0]); // username tạm từ email
            account.setPassword(null); // OAuth không cần password
            account.setStatus("ACTIVE");

            Role role = roleRepository.findByRoleName("Customer")
                    .orElseGet(() -> roleRepository.findById(3).orElse(null));

            account.setRole(role);

            Account saved = accountRepository.save(account);

            // tạo customer tương ứng
            Customer customer = new Customer();
            customer.setAccount(saved);
            customer.setFullName(name);
            customer.setEmail(email);
            customer.setCreatedDate(new java.sql.Date(System.currentTimeMillis()));

            customerRepository.save(customer);
        }
    }
}
