package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.fpt.fashionstore.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    // LOGIN
    Optional<Account> findByUsername(String username);

    Optional<Account> findByEmail(String email);

    // CHECK TRÙNG
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // CHECK TRÙNG KHI UPDATE (TRỪ CHÍN NÓ)
    boolean existsByUsernameAndAccountIdNot(String username, Integer accountId);

    boolean existsByEmailAndAccountIdNot(String email, Integer accountId);

    boolean existsByPhoneAndAccountIdNot(String phone, Integer accountId);

    // LẤY TẤT CẢ ACCOUNT KHÔNG BỊ DELETED (PHÂN TRANG)
    Page<Account> findByStatusIn(
            List<String> statuses,
            Pageable pageable
    );

    // SEARCH ACCOUNT ACTIVE + LOCKED
    @Query("SELECT a FROM Account a " +
        "WHERE a.status IN ('ACTIVE', 'LOCKED') " +
        "AND (" +
        "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        ")")
    Page<Account> searchNotDeleted(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // LẤY ACCOUNT INACTIVE (ĐÃ XÓA)
    Page<Account> findByStatus(
            String status,
            Pageable pageable
    );

    // SEARCH ACCOUNT INACTIVE
    @Query("SELECT a FROM Account a " +
        "WHERE a.status = 'INACTIVE' " +
        "AND (" +
        "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        ")")
    Page<Account> searchDeleted(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // LẤY ACCOUNT THEO ROLE
    List<Account> findByRole_RoleName(String roleName);



    /* =================================================
                STAFF MANAGEMENT (ADMIN)
       ================================================= */

    // LẤY DANH SÁCH NHÂN VIÊN (SALE, STOCK, SUPPORT, MANAGER)
    @Query("SELECT a FROM Account a " +
        "WHERE a.status IN ('ACTIVE','LOCKED','LEAVE') " +
        "AND a.role.roleId IN (3,4,5,6)")
    Page<Account> findStaffAccounts(Pageable pageable);

    // SEARCH NHÂN VIÊN
    @Query("SELECT a FROM Account a " +
        "WHERE a.status IN ('ACTIVE','LOCKED','LEAVE') " +
        "AND a.role.roleId IN (3,4,5,6) " +
        "AND (" +
        "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        ")")
    Page<Account> searchStaffAccounts(
            @Param("keyword") String keyword,
            Pageable pageable
    );

}