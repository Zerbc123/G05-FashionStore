package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Voucher;
import vn.edu.fpt.fashionstore.service.VoucherService;

@Controller
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    @Autowired
    private VoucherService voucherService;

    // 1. Hiển thị danh sách Voucher
    @GetMapping
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherService.getAllVouchers());
        return "admin/manage-vouchers";
    }

    // 2. Mở form thêm mới Voucher
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("voucher", new Voucher());
        model.addAttribute("pageTitle", "Thêm Mã Giảm Giá Mới");
        return "admin/voucher-form"; // Trỏ tới file HTML chứa Form
    }

    // 3. Mở form sửa Voucher
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes ra) {
        Voucher voucher = voucherService.getVoucherById(id);
        if (voucher == null) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy mã giảm giá!");
            return "redirect:/admin/vouchers";
        }
        model.addAttribute("voucher", voucher);
        model.addAttribute("pageTitle", "Cập Nhật Mã Giảm Giá");
        return "admin/voucher-form";
    }

    // 4. Xử lý lưu dữ liệu (Dùng chung cho cả Thêm và Sửa - KHÔNG JS)
    @PostMapping("/save")
    public String saveVoucher(@ModelAttribute("voucher") Voucher voucher, RedirectAttributes ra) {
        // Nếu là tạo mới (ID rỗng) thì phải check xem mã có bị trùng không
        if (voucher.getVoucherId() == null && voucherService.checkCodeExists(voucher.getCode())) {
            ra.addFlashAttribute("errorMessage", "Lỗi: Mã '" + voucher.getCode() + "' đã tồn tại! Vui lòng chọn mã khác.");
            return "redirect:/admin/vouchers/add";
        }

        voucherService.saveVoucher(voucher);
        ra.addFlashAttribute("successMessage", "Đã lưu mã giảm giá thành công!");
        return "redirect:/admin/vouchers";
    }

    // 5. Xóa Voucher (Gọi qua đường link GET tĩnh)
    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Integer id, RedirectAttributes ra) {
        voucherService.deleteVoucher(id);
        ra.addFlashAttribute("successMessage", "Đã xóa mã giảm giá khỏi hệ thống!");
        return "redirect:/admin/vouchers";
    }
}