package vn.edu.fpt.fashionstore.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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
        // Lưu ý: Nếu dùng Google Login, password có thể null. Cần check null trước khi .equals()
        if (account.getPassword() == null || !account.getPassword().equals(password)) {
            return null; // Sai mật khẩu hoặc tài khoản này chỉ dùng login qua Google
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
        Integer phoneNumber = null;
        if (phone != null && !phone.trim().isEmpty()) {
            try {
                String cleanPhone = phone.replaceAll("[^0-9]", "");
                if (!cleanPhone.isEmpty()) {
                    phoneNumber = Integer.parseInt(cleanPhone);
                }
            } catch (NumberFormatException e) {
                phoneNumber = null;
            }
        }

        // 3. Khởi tạo Account
        Account newAccount = new Account();
        newAccount.setUsername(finalUsername);
        newAccount.setEmail(email);
        newAccount.setPassword(password);
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
            try {
                // Loại bỏ ký tự không phải số trước khi parse
                if (phone != null && !phone.isEmpty()) {
                    account.setPhone(Integer.parseInt(phone.replaceAll("[^0-9]", "")));
                }
            } catch (Exception e) {
                // Log lỗi nếu cần: System.out.println("Lỗi format số điện thoại");
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

                try {
                    if (phone != null) {
                        customer.setPhone(Integer.parseInt(phone.replaceAll("[^0-9]", "")));
                    }
                } catch (Exception e) {}

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
        return existAccount.get();
    }

}
