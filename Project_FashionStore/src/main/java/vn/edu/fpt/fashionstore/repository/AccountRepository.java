package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.fashionstore.entity.Account;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // LẤY DANH SÁCH STAFF
    List<Account> findByRole_RoleName(String roleName);
    
    // TÌM KIẾM NHÂN VIÊN THEO TÊN, EMAIL, USERNAME
    Page<Account> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
        String fullName, String email, String username, Pageable pageable);
}
