package vn.edu.fpt.fashionstore.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Role;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.repository.RoleRepository;

import java.util.Date;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;




    // Khai báo Enum hoặc Constant
    public static final String ROLE_CUSTOMER = "Customer";

    /**
     * Xác thực đăng nhập
     * @param username - username hoặc email
     * @param password - mật khẩu (plain text)
     * @return Account nếu đăng nhập thành công, null nếu thất bại
     */
    public Account authenticate(String username, String password) {

        // 1. Tìm account theo Username HOẶC Email
        Account account = accountRepository.findByUsername(username)
                .orElseGet(() -> accountRepository.findByEmail(username).orElse(null));

        if (account == null) {
            return null; // Không tìm thấy tài khoản
        }

        if (account.getPassword() == null) {
            return null; // Tài khoản này chỉ dùng login qua Google
        }

        // 2. Kiểm tra password (Xử lý dứt điểm cả Hash BCrypt lẫn Chữ thô)
        boolean passwordValid = false;

        if (account.getPassword().startsWith("$2a$")) {
            // Nếu trong DB là chuỗi mã hóa BCrypt
            passwordValid = passwordEncoder.matches(password, account.getPassword());
        } else {
            // Nếu trong DB là chữ thô (như '123456' bạn vừa update trong SQL)
            passwordValid = account.getPassword().equals(password);
        }

        if (!passwordValid) {
            return null; // Sai mật khẩu
        }

        // 3. Kiểm tra status
        if (!"Active".equalsIgnoreCase(account.getStatus())) {
            return null; // Tài khoản bị khóa hoặc chưa kích hoạt
        }

        return account;
    }


    /**
     * Tìm account theo username
     */
    public Optional<Account> findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    /**
     * Tìm account theo email
     */
    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    /**
     * Lưu account mới (đăng ký)
     */
    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }


    /**
     * Kiểm tra email đã tồn tại chưa
     */
    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    /**
     * Đăng ký tài khoản mới
     * @param email - Email
     * @param password - Mật khẩu
     * @param fullName - Tên đầy đủ
     * @param phone - Số điện thoại (String)
     * @return Account nếu đăng ký thành công, null nếu thất bại
     */
    public Account registerAccount(String email, String password, String fullName, String phone) {
        if (accountRepository.existsByEmail(email)) {
            return null;
        }

        // 1. Tạo Username
        String baseUsername = email.split("@")[0];
        String finalUsername = baseUsername;
        int count = 1;
        while (accountRepository.existsByUsername(finalUsername)) {
            finalUsername = baseUsername + count++;
        }

        // 2. Xử lý Phone (giữ dưới dạng chuỗi chỉ chứa chữ số)
        String phoneNumber = null;
        if (phone != null && !phone.trim().isEmpty()) {
            // loại bỏ ký tự khác số để chuẩn hóa
            phoneNumber = phone.replaceAll("[^0-9]", "");
            if (phoneNumber.isEmpty()) {
                phoneNumber = null;
            }
        }

        // 3. Khởi tạo Account
        Account newAccount = new Account();
        newAccount.setUsername(finalUsername);
        newAccount.setEmail(email);
        // Hash password trước khi lưu
        if (password != null && !password.equals("OAUTH2_USER")) {
            newAccount.setPassword(passwordEncoder.encode(password));
        } else {
            newAccount.setPassword(password); // For OAuth2 users
        }
        newAccount.setFullName(fullName);
        newAccount.setPhone(phoneNumber);
        newAccount.setStatus("active");

        Role customerRole = roleRepository.findByRoleName("Customer")
                .orElseGet(() -> roleRepository.findById(3).orElse(null));
        newAccount.setRole(customerRole);

        // Lưu Account trước
        Account savedAccount = accountRepository.save(newAccount);

        // --- ĐOẠN MỚI THÊM: TẠO CUSTOMER ĐỒNG BỘ ---
        if (customerRole != null && "Customer".equalsIgnoreCase(customerRole.getRoleName())) {
            Customer customer = new Customer();
            customer.setAccount(savedAccount); // Link tới Account vừa tạo
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setPhone(phoneNumber); // Đồng bộ số điện thoại sang bảng Customer
            customer.setCreatedDate(new Date());

            customerRepository.save(customer); // Lưu vào bảng Customer
        }
        // ------------------------------------------

        return savedAccount;
    }

    @Transactional
    public Account updateProfile(String email, String fullName, String phone, String address, String gender, Date dateOfBirth) {
        return accountRepository.findByEmail(email).map(account -> {
            // 1. Cập nhật thông tin vào bảng Account
            account.setFullName(fullName);
            // Loại bỏ ký tự không phải số để lưu chuỗi chỉ gồm số
            if (phone != null && !phone.isEmpty()) {
                account.setPhone(phone.replaceAll("[^0-9]", ""));
            }

            // Lưu Account trước để có ID ổn định
            Account savedAccount = accountRepository.save(account);

            // 2. Cập nhật thông tin vào bảng Customer
            // Kiểm tra Role (tớ giữ nguyên logic check Role của bạn)
            if (account.getRole() != null && "Customer".equalsIgnoreCase(account.getRole().getRoleName())) {

                Customer customer;
                if (account.getCustomers() != null && !account.getCustomers().isEmpty()) {
                    customer = account.getCustomers().get(0);
                } else {
                    customer = new Customer();
                    customer.setCreatedDate(new Date());
                    customer.setAccount(savedAccount);
                }

                // Gán dữ liệu mới cho Customer
                customer.setFullName(fullName);
                customer.setEmail(email);
                customer.setAddress(address);
                // Giả sử: true = Nam (1), false = Nữ (0)
                if (gender != null) {
                    if (gender.equals("Nam")) {
                        customer.setGender(true);
                    } else if (gender.equals("Nữ")) {
                        customer.setGender(false);
                    }
                }    // <--- MỚI THÊM
                customer.setDateOfBirth(dateOfBirth); // <--- MỚI THÊM

                if (phone != null) {
                    customer.setPhone(phone.replaceAll("[^0-9]", ""));
                }

                // Lưu bảng Customer
                customerRepository.save(customer);
            }
            return savedAccount;
        }).orElse(null);
    }


    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Tìm user trong DB theo Email (là username)
        vn.edu.fpt.fashionstore.entity.Account acc = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Trả về User của Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(acc.getEmail())
                .password(acc.getPassword()) // Vì dùng NoOp nên nó sẽ so sánh trực tiếp chữ thường
                .roles("USER")
                .build();
    }


    @Transactional
    public Account processOAuthPostLogin(String email, String fullName) {
        Optional<Account> existAccount = accountRepository.findByEmail(email);

        if (existAccount.isEmpty()) {
            // Tự động tạo Username từ Email (giống logic ông đã viết)
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
            newAccount.setPassword(null); // Không có mật khẩu cho User Google
            newAccount.setStatus("active");

            // Gán Role Customer (Lấy ID 3 hoặc tìm theo tên như ông đã làm)
            Role customerRole = roleRepository.findByRoleName("Customer")
                    .orElseGet(() -> roleRepository.findById(3).orElse(null));
            newAccount.setRole(customerRole);

            Account savedAccount = accountRepository.save(newAccount);

            // Tạo luôn bản ghi bên bảng Customer để đồng bộ
            Customer customer = new Customer();
            customer.setAccount(savedAccount);
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setCreatedDate(new Date());
            customerRepository.save(customer);

            return savedAccount;
        }
        // Account đã tồn tại: backfill Customer nếu thiếu
        Account account = existAccount.get();
        boolean isCustomerRole = account.getRole() != null &&
                "Customer".equalsIgnoreCase(account.getRole().getRoleName());
        boolean hasNoCustomer = account.getCustomers() == null || account.getCustomers().isEmpty();

        if (isCustomerRole && hasNoCustomer) {
            Customer customer = new Customer();
            customer.setAccount(account);
            customer.setFullName(fullName != null ? fullName : account.getFullName());
            customer.setEmail(email);
            customer.setCreatedDate(new Date());
            customerRepository.save(customer);
        }
        return account;
    }

    /**
     * Đổi mật khẩu người dùng
     * @param email - Email người dùng
     * @param currentPassword - Mật khẩu hiện tại
     * @param newPassword - Mật khẩu mới
     * @return true nếu thành công, false nếu thất bại
     */
    @Transactional
    public boolean changePassword(String email, String currentPassword, String newPassword) {
        try {
            // 1. Tìm account theo email
            Optional<Account> accountOpt = accountRepository.findByEmail(email);
            if (!accountOpt.isPresent()) {
                return false;
            }

            Account account = accountOpt.get();

            // 2. Kiểm tra mật khẩu hiện tại
            if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
                return false;
            }

            // 3. Hash mật khẩu mới và lưu
            account.setPassword(passwordEncoder.encode(newPassword));
            accountRepository.save(account);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    public Account getAccountByEmail(String email) {
        return accountRepository.findByEmail(email).orElse(null);
    }

}