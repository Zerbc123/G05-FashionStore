package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Voucher;
import vn.edu.fpt.fashionstore.repository.VoucherRepository;

import java.util.List;
import java.util.Optional;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    // 1. Lấy toàn bộ danh sách Voucher để hiển thị lên bảng cho Admin
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    // 2. Thêm mới hoặc Cập nhật Voucher
    public Voucher saveVoucher(Voucher voucher) {
        // Tự động chuyển mã code thành CHỮ IN HOA và xóa khoảng trắng thừa cho chuẩn mực
        if (voucher.getCode() != null) {
            voucher.setCode(voucher.getCode().trim().toUpperCase());
        }
        return voucherRepository.save(voucher);
    }

    // 3. Tìm 1 Voucher theo ID (Dùng khi Admin bấm nút "Sửa")
    public Voucher getVoucherById(Integer id) {
        Optional<Voucher> optional = voucherRepository.findById(id);
        return optional.orElse(null); // Trả về null nếu không tìm thấy
    }

    // 4. Xóa Voucher theo ID
    public void deleteVoucher(Integer id) {
        voucherRepository.deleteById(id);
    }

    // 5. Hàm hỗ trợ kiểm tra mã trùng lặp trước khi lưu
    public boolean checkCodeExists(String code) {
        return voucherRepository.existsByCode(code.trim().toUpperCase());
    }

    // Lấy danh sách mã giảm giá hợp lệ cho Khách hàng xem
    public List<Voucher> getValidVouchersForCustomer() {
        // Lấy ngày giờ hiện tại
        java.util.Date today = new java.util.Date();
        return voucherRepository.findByIsActiveTrueAndExpiredDateGreaterThanEqual(today);
    }

    public Voucher getValidVoucherByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;

        Optional<Voucher> optVoucher = voucherRepository.findByCode(code.trim().toUpperCase());

        if (optVoucher.isPresent()) {
            Voucher v = optVoucher.get();

            // Ép thời gian hiện tại về 00:00:00 để so sánh công bằng với Database
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            java.util.Date todayMidnight = cal.getTime();

            // Kiểm tra xem mã có đang kích hoạt và còn hạn (Tính đến hết 23h59p của ngày hết hạn)
            if (v.getIsActive() != null && v.getIsActive() && !v.getExpiredDate().before(todayMidnight)) {
                return v;
            }
        }
        return null;
    }
}