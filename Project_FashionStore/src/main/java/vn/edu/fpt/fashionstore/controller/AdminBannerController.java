package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Banner;
import vn.edu.fpt.fashionstore.repository.BannerRepository;

@Controller
@RequestMapping("/admin/banners")
public class AdminBannerController {

    @Autowired
    private BannerRepository bannerRepository;

    // 1. Hiển thị danh sách Banner
    @GetMapping
    public String listBanners(Model model) {
        model.addAttribute("banners", bannerRepository.findAllByOrderByDisplayOrderAsc());
        return "admin/managebanners";
    }

    // 2. Mở form thêm Banner mới
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("banner", new Banner());
        return "admin/bannerform";
    }

    // 3. Mở form sửa Banner
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Banner: " + id));
        model.addAttribute("banner", banner);
        return "admin/bannerform";
    }

    // 4. Lưu Banner (dùng chung cho Thêm và Sửa)
    @PostMapping("/save")
    public String saveBanner(@ModelAttribute("banner") Banner banner, RedirectAttributes ra) {
        bannerRepository.save(banner);
        ra.addFlashAttribute("successMessage", "Đã lưu Banner thành công!");
        return "redirect:/admin/banners";
    }

    // 5. Xóa Banner
    @GetMapping("/delete/{id}")
    public String deleteBanner(@PathVariable("id") Long id, RedirectAttributes ra) {
        bannerRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Đã xóa Banner thành công!");
        return "redirect:/admin/banners";
    }

    // 6. Tắt / Bật Banner nhanh
    @GetMapping("/toggle/{id}")
    public String toggleBannerStatus(@PathVariable("id") Long id, RedirectAttributes ra) {
        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner != null) {
            banner.setIsActive(!banner.getIsActive()); // Đảo ngược trạng thái
            bannerRepository.save(banner);
            ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái Banner!");
        }
        return "redirect:/admin/banners";
    }
}