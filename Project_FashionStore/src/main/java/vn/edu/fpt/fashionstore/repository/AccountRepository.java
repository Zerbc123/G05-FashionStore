package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.fpt.fashionstore.entity.Account;
import org.springframework.data.repository.query.Param;
import java.util.List;


public interface AccountRepository extends JpaRepository<Account, Integer> {

    // CHECK TRÙNG
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // LẤY TẤT CẢ ACCOUNT KHÔNG BỊ DELETED (PHÂN TRANG)
    Page<Account> findByStatusIn(
            List<String> statuses,
            Pageable pageable
    );


    // SEARCH
    @Query("""
    SELECT a FROM Account a
    WHERE a.status IN ('ACTIVE', 'LOCKED')
    AND (
        LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    Page<Account> searchNotDeleted(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // LẤY TẤT CẢ ACCOUNT BỊ XÓA (INACTIVE) - PHÂN TRANG
    Page<Account> findByStatus(
            String status,
            Pageable pageable
    );

    // TÌM KIẾM ACCOUNT BỊ XÓA (INACTIVE)
    @Query("""
    SELECT a FROM Account a
    WHERE a.status = 'INACTIVE'
    AND (
        LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    Page<Account> searchDeleted(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<Account> findByRole_RoleName(String roleName);
}
