package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.ReturnRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    // Tìm yêu cầu trả hàng dựa vào đơn hàng (Để kiểm tra xem đơn này đã bị yêu cầu trả chưa)
    Optional<ReturnRequest> findByOrder(Order order);

    // Lấy danh sách các yêu cầu trả hàng của một khách hàng cụ thể
    List<ReturnRequest> findByCustomerOrderByRequestDateDesc(Customer customer);

    // Thêm dòng này vào
    List<ReturnRequest> findAllByOrderByRequestDateDesc();
}