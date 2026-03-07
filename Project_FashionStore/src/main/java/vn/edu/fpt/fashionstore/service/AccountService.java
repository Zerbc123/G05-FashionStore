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

import java.time.LocalDate;
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

        // 1. Tìm account theo email
        Optional<Account> accountOpt = accountRepository.findByEmail(username);

        if (accountOpt.isEmpty()) {
            return null; // Email không tồn tại
        }

        Account account = accountOpt.get();

        // 2. Kiểm tra password
        // Xử lý cả plain text và hashed passwords
        boolean passwordValid = false;

        if (account.getPassword() == null) {
            return null; // Tài khoản này chỉ dùng login qua Google
        }

        // Thử verify với BCrypt trước (cho passwords đã được mã hóa)
        try {
            passwordValid = passwordEncoder.matches(password, account.getPassword());
        } catch (Exception e) {
            // Nếu có lỗi (có thể do password không được hash), thử so sánh plain text
            passwordValid = account.getPassword().equals(password);
        }

        if (!passwordValid) {
            return null; // Sai mật khẩu
        }

        // 3. Kiểm tra status (dùng .equalsIgnoreCase để tránh lỗi viết hoa/thường)
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

        // 2. Xử lý Phone
        String phoneNumber = null;
        if (phone != null && !phone.trim().isEmpty()) {
            try {
                String cleanPhone = phone.replaceAll("[^0-9]", "");
                if (!cleanPhone.isEmpty()) {
                    phoneNumber = cleanPhone;
                }
            } catch (Exception e) {
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
            customer.setCreatedDate(LocalDate.now());

            customerRepository.save(customer); // Lưu vào bảng Customer
        }
        // ------------------------------------------

        return savedAccount;
    }

    @Transactional
    public Account updateProfile(String email, String fullName, String phone, String address, String gender, LocalDate dateOfBirth) {
        return accountRepository.findByEmail(email).map(account -> {
            // 1. Cập nhật thông tin vào bảng Account
            account.setFullName(fullName);
            // Xử lý phone như String
            if (phone != null && !phone.trim().isEmpty()) {
                String cleanPhone = phone.replaceAll("[^0-9]", "");
                account.setPhone(cleanPhone.isEmpty() ? null : cleanPhone);
            } else {
                account.setPhone(null);
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
                    customer.setCreatedDate(LocalDate.now());
                    customer.setAccount(savedAccount);
                }

                // Gán dữ liệu mới cho Customer
                customer.setFullName(fullName);
                customer.setEmail(email);
                customer.setAddress(address);
                // Giả sử: true = Nam (1), false = Nữ (0)
                if (gender != null) {
                    if (gender.equalsIgnoreCase("Nam")) {
                        customer.setGender(true);
                    } else if (gender.equalsIgnoreCase("Nữ")) {
                        customer.setGender(false);
                    }
                }
                customer.setDateOfBirth(dateOfBirth);

                // Xử lý phone như String
                if (phone != null && !phone.trim().isEmpty()) {
                    String cleanPhone = phone.replaceAll("[^0-9]", "");
                    customer.setPhone(cleanPhone.isEmpty() ? null : cleanPhone);
                } else {
                    customer.setPhone(null);
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

        // Get role name from account
        String roleName = (acc.getRole() != null) ? acc.getRole().getRoleName() : "USER";
        
        // Trả về User của Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(acc.getEmail())
                .password(acc.getPassword())
                .roles(roleName)
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
            customer.setCreatedDate(LocalDate.now());
            customerRepository.save(customer);

            return savedAccount;
        }
        return existAccount.get();
    }

}