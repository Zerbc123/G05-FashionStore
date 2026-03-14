package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.ReturnRequest;
import vn.edu.fpt.fashionstore.entity.ReturnStatus;
import vn.edu.fpt.fashionstore.repository.ReturnRequestRepository;

import java.util.Date;
import java.util.List;

@Service
public class ReturnRequestService {

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    // Kiểm tra xem đơn hàng này đã từng bị yêu cầu trả hàng chưa
    public boolean hasReturnRequest(Order order) {
        return returnRequestRepository.findByOrder(order).isPresent();
    }

    // Lấy thông tin yêu cầu trả hàng của đơn hàng (nếu có)
    public ReturnRequest getReturnRequestByOrder(Order order) {
        return returnRequestRepository.findByOrder(order).orElse(null);
    }

    // Tạo yêu cầu trả hàng mới
    public ReturnRequest createReturnRequest(Order order, Customer customer, String reason, String description) {
        if (hasReturnRequest(order)) {
            throw new RuntimeException("Đơn hàng này đã được yêu cầu trả hàng trước đó!");
        }

        ReturnRequest request = new ReturnRequest();
        request.setOrder(order);
        request.setCustomer(customer);
        request.setReason(reason);
        request.setDescription(description);
        request.setStatus(ReturnStatus.PENDING); // Vừa tạo là trạng thái Chờ xử lý
        request.setRequestDate(new Date());

        return returnRequestRepository.save(request);
    }

    // Thêm 2 hàm này cho Admin
    public List<ReturnRequest> getAllReturnRequestsForAdmin() {
        return returnRequestRepository.findAllByOrderByRequestDateDesc();
    }

    public void updateReturnStatus(Long returnId, ReturnStatus newStatus, String adminNote) {
        ReturnRequest request = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đổi trả!"));
        request.setStatus(newStatus);
        if (adminNote != null && !adminNote.trim().isEmpty()) {
            request.setAdminNote(adminNote);
        }
        returnRequestRepository.save(request);
    }
}