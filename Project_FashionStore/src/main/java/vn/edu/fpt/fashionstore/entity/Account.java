package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private int accountId;
    private String username;
    private String password;
    private String email;
    private String status;
    @ManyToOne
    @JoinColumn (name = "role_id")
    private Role role;

    @Column (name = "full_name")
    private String fullName;
    // số điện thoại lưu dưới dạng chuỗi để giữ các ký tự 0 đầu
    private String phone; // String có thể rỗng hoặc null

    @OneToMany (mappedBy = "account")
    private List<Customer> customers;

    public Account() {
    }

    public Account(int accountId, String username, String password, String status, String email, Role role, String fullName, String phone) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.status = status;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.phone = phone;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }
}