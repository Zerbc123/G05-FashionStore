package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Banner;
import vn.edu.fpt.fashionstore.repository.BannerRepository;
import vn.edu.fpt.fashionstore.service.ImageUploadService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/banners")
public class AdminBannerController {

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private ImageUploadService imageUploadService;

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
    public String saveBanner(@ModelAttribute("banner") Banner banner, 
                             @RequestParam("imageFile") MultipartFile imageFile,
                             RedirectAttributes ra) {
        try {
            // Nếu có file ảnh được upload, upload lên Cloudinary và lấy URL
            if (imageFile != null && !imageFile.isEmpty()) {
                // Check file size (10MB = 10 * 1024 * 1024 bytes)
                if (imageFile.getSize() > 10 * 1024 * 1024) {
                    ra.addFlashAttribute("errorMessage", "Kích thước file quá lớn! Vui lòng chọn file ảnh nhỏ hơn 10MB.");
                    return "redirect:/admin/banners" + (banner.getBannerId() != null ? "/edit/" + banner.getBannerId() : "/add");
                }
                
                if (imageUploadService.isValidImageFile(imageFile)) {
                    String imageUrl = imageUploadService.uploadImage(imageFile);
                    banner.setImageUrl(imageUrl);
                } else {
                    ra.addFlashAttribute("errorMessage", "File không hợp lệ! Vui lòng chọn file ảnh (JPG, PNG, GIF, WebP).");
                    return "redirect:/admin/banners" + (banner.getBannerId() != null ? "/edit/" + banner.getBannerId() : "/add");
                }
            }
            // Nếu không có file ảnh mới nhưng banner đã có imageUrl từ trước, giữ nguyên
            else if (banner.getImageUrl() == null || banner.getImageUrl().isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng chọn ảnh cho banner!");
                return "redirect:/admin/banners" + (banner.getBannerId() != null ? "/edit/" + banner.getBannerId() : "/add");
            }
            
            bannerRepository.save(banner);
            ra.addFlashAttribute("successMessage", "Đã lưu Banner thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi upload ảnh: " + e.getMessage());
            return "redirect:/admin/banners" + (banner.getBannerId() != null ? "/edit/" + banner.getBannerId() : "/add");
        }
        
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

    // Handle file upload size exceeded exception
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxSizeException(MaxUploadSizeExceededException exc, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("errorMessage", "Kích thước file quá lớn! Vui lòng chọn file ảnh nhỏ hơn 10MB.");
        return new ModelAndView("redirect:/admin/banners/add");
    }
}