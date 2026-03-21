package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.ReturnRequestRepository;
import vn.edu.fpt.fashionstore.repository.ProductVariantRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReturnRequestService {

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

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
        request.setRequestDate(LocalDate.now());

        return returnRequestRepository.save(request);
    }

    // Thêm 2 hàm này cho Admin
    public List<ReturnRequest> getAllReturnRequestsForAdmin() {
        return returnRequestRepository.findAllByOrderByRequestDateDesc();
    }

    public void updateReturnStatus(Long returnId, ReturnStatus newStatus, String adminNote) {
        ReturnRequest request = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đổi trả!"));
        
        ReturnStatus oldStatus = request.getStatus();
        request.setStatus(newStatus);
        if (adminNote != null && !adminNote.trim().isEmpty()) {
            request.setAdminNote(adminNote);
        }
        returnRequestRepository.save(request);
        
        // Nếu trả hàng được chấp thuận, hoàn lại stock
        if (oldStatus != ReturnStatus.APPROVED && newStatus == ReturnStatus.APPROVED) {
            Order order = request.getOrder();
            for (OrderItem orderItem : order.getOrderItems()) {
                ProductVariant variant = orderItem.getProductVariant();
                variant.setStock(variant.getStock() + orderItem.getQuantity());
                productVariantRepository.save(variant);
                
            }
        }
    }
}