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
        return accountRepository.findByStatusIn(List.of("ACTIVE","LOCKED"), pageable);
    }

    public Page<Account> searchStaff(String keyword,int page,int size){
        Pageable pageable = PageRequest.of(page,size,Sort.by("accountId").descending());

        if(keyword == null || keyword.trim().isEmpty()){
            return getAllStaff(page,size);
        }

        return accountRepository.searchNotDeleted(keyword.trim(), pageable);
    }

    public Role getRoleById(Integer roleId){
        return roleRepository.findById(roleId).orElse(null);
    }

    public void createStaff(Account account,Integer roleId){

        if(accountRepository.existsByUsername(account.getUsername()))
            throw new RuntimeException("Username đã tồn tại");

        if(accountRepository.existsByEmail(account.getEmail()))
            throw new RuntimeException("Email đã tồn tại");

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

    public List<Account> getSupportStaff(){
        return accountRepository.findByRole_RoleName("SUPPORT");
    }

    public boolean isStaffAssigned(Integer staffId){
        return supportRequestRepository.existsByAssignedStaffId(staffId);
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

    /* =================================================
                        REGISTER
       ================================================= */

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
        customer.setCreatedDate(new Date());

        customerRepository.save(customer);

        return saved;
    }

    /* =================================================
                        PROFILE
       ================================================= */

    @Transactional
    public Account updateProfile(String email,String fullName,String phone){

        Optional<Account> accountOpt = accountRepository.findByEmail(email);

        if(accountOpt.isEmpty()) return null;

        Account account = accountOpt.get();

        account.setFullName(fullName);
        account.setPhone(phone);

        return accountRepository.save(account);
    }

    /* =================================================
                        CHANGE PASSWORD
       ================================================= */

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

}