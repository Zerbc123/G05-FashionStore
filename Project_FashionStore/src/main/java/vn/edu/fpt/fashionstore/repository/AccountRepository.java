package vn.edu.fpt.fashionstore.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Account;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    
    // Tìm account theo username
    Optional<Account> findByUsername(String username);
    
    // Tìm account theo email
    Optional<Account> findByEmail(String email);
    
    // Kiểm tra username đã tồn tại chưa
    boolean existsByUsername(String username);
    
    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);


}
