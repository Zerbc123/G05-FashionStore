package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.fashionstore.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    // tìm role theo tên (ADMIN, STAFF, CUSTOMER)
    Role findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);
}
