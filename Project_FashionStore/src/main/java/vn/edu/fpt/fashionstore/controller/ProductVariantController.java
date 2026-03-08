package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.service.ProductVariantService;

import java.util.List;

@Controller
@RequestMapping("/admin/product-variants")
public class ProductVariantController {

    @Autowired
    private ProductVariantService productVariantService;

    @GetMapping
    public String listVariants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "variantId") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        
        // Sort
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortDirection, sort));
        
        // Get paginated variants
        Page<ProductVariant> variantPage = productVariantService.getAllVariants(pageable);
        List<ProductVariant> variants = variantPage.getContent();
        
        // Calculate statistics
        long totalVariants = variantPage.getTotalElements();
        long inStockCount = variants.stream().filter(v -> v.getStock() > 20).count();
        long lowStockCount = variants.stream().filter(v -> v.getStock() > 0 && v.getStock() <= 20).count();
        long outOfStockCount = variants.stream().filter(v -> v.getStock() == 0).count();
        
        model.addAttribute("variants", variants);
        model.addAttribute("variantPage", variantPage);
        model.addAttribute("totalVariants", totalVariants);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        
        return "admin/productvariants";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("variant", new ProductVariant());
        model.addAttribute("products", productVariantService.getAllProducts());
        model.addAttribute("colors", productVariantService.getAllColors());
        model.addAttribute("categorySizes", productVariantService.getAllCategorySizes());
        model.addAttribute("isEdit", false);
        return "admin/productvariant-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        var variant = productVariantService.getVariantById(id);
        if (variant.isPresent()) {
            model.addAttribute("variant", variant.get());
            model.addAttribute("products", productVariantService.getAllProducts());
            model.addAttribute("colors", productVariantService.getAllColors());
            model.addAttribute("categorySizes", productVariantService.getAllCategorySizes());
            model.addAttribute("isEdit", true);
            return "admin/productvariant-form";
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể sản phẩm");
            return "redirect:/admin/product-variants";
        }
    }

    @PostMapping("/save")
    public String saveVariant(@ModelAttribute ProductVariant variant,
                             @RequestParam Long productId,
                             @RequestParam int colorId,
                             @RequestParam int categorySizeId,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            Product product = new Product();
            product.setProductId(productId);
            variant.setProduct(product);

            Color color = new Color();
            color.setColorId(colorId);
            variant.setColor(color);

            CategorySize categorySize = new CategorySize();
            categorySize.setCategorySizeId(categorySizeId);
            variant.setCategorySize(categorySize);

            productVariantService.createVariant(variant);
            redirectAttributes.addFlashAttribute("success", "Thêm biến thể sản phẩm thành công");
            return "redirect:/admin/product-variants";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi thêm biến thể: " + e.getMessage());
            model.addAttribute("products", productVariantService.getAllProducts());
            model.addAttribute("colors", productVariantService.getAllColors());
            model.addAttribute("categorySizes", productVariantService.getAllCategorySizes());
            model.addAttribute("isEdit", false);
            return "admin/productvariant-form";
        }
    }

    @PostMapping("/update/{id}")
    public String updateVariant(@PathVariable int id,
                               @ModelAttribute ProductVariant variant,
                               @RequestParam Long productId,
                               @RequestParam int colorId,
                               @RequestParam int categorySizeId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Product product = new Product();
            product.setProductId(productId);
            variant.setProduct(product);

            Color color = new Color();
            color.setColorId(colorId);
            variant.setColor(color);

            CategorySize categorySize = new CategorySize();
            categorySize.setCategorySizeId(categorySizeId);
            variant.setCategorySize(categorySize);

            ProductVariant updated = productVariantService.updateVariant(id, variant);
            if (updated != null) {
                redirectAttributes.addFlashAttribute("success", "Cập nhật biến thể sản phẩm thành công");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể sản phẩm");
            }
            return "redirect:/admin/product-variants";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi cập nhật biến thể: " + e.getMessage());
            model.addAttribute("products", productVariantService.getAllProducts());
            model.addAttribute("colors", productVariantService.getAllColors());
            model.addAttribute("categorySizes", productVariantService.getAllCategorySizes());
            model.addAttribute("isEdit", true);
            return "admin/productvariant-form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteVariant(@PathVariable int id, RedirectAttributes redirectAttributes) {
        String result = productVariantService.deleteVariantWithOrderCheck(id);
        
        if (result.equals("success")) {
            redirectAttributes.addFlashAttribute("success", "Xóa biến thể sản phẩm thành công");
        } else if (result.equals("Variant not found")) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể sản phẩm");
        } else if (result.contains("linked to existing orders")) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa biến thể: Biến thể này đã được sử dụng trong đơn hàng");
        } else {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa biến thể: " + result);
        }
        
        return "redirect:/admin/product-variants";
    }

    @GetMapping("/product/{productId}")
    public String getVariantsByProduct(@PathVariable Long productId, Model model) {
        List<ProductVariant> variants = productVariantService.getVariantsByProductId(productId);
        model.addAttribute("variants", variants);
        model.addAttribute("productId", productId);
        return "admin/productvariants :: variantList";
    }
}
