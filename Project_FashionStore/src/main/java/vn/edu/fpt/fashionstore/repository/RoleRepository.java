package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // Tìm role theo tên (ADMIN, STAFF, CUSTOMER)
    Optional<Role> findByRoleName(String roleName);

    // Kiểm tra role đã tồn tại chưa
    boolean existsByRoleName(String roleName);

}