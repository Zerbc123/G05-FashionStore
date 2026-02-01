package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.service.ProductService;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public String showProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Boolean isNew,
            @RequestParam(required = false) Boolean isSale,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        // Tạo Pageable với phân trang và sắp xếp
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortDirection, sort));

        Page<Product> productPage;

        // Nếu có từ khóa tìm kiếm hoặc bộ lọc, sử dụng searchAndFilterProducts
        if (hasSearchOrFilter(keyword, category, minPrice, maxPrice, size, isNew, isSale)) {
            productPage = productService.searchAndFilterProducts(
                keyword, category, minPrice, maxPrice, size, isNew, isSale, pageable);
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        if (page > 0 && productPage.getNumberOfElements() == 0 && productPage.getTotalElements() > 0) {
            page = 0;
            pageable = PageRequest.of(page, pageSize, Sort.by(sortDirection, sort));
            if (hasSearchOrFilter(keyword, category, minPrice, maxPrice, size, isNew, isSale)) {
                productPage = productService.searchAndFilterProducts(
                    keyword, category, minPrice, maxPrice, size, isNew, isSale, pageable);
            } else {
                productPage = productService.getAllProducts(pageable);
            }
        }

        System.out.println("/products loaded elements=" + productPage.getNumberOfElements() + ", total=" + productPage.getTotalElements() + ", page=" + page);

        // Lấy danh sách danh mục và kích thước cho bộ lọc
        List<String> categories = productService.getAllCategories();
        List<String> sizes = productService.getAllSizes();

        // Thêm dữ liệu vào model
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        // Thêm các tham số tìm kiếm và lọc vào model
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedSize", size);
        model.addAttribute("isNew", isNew);
        model.addAttribute("isSale", isSale);

        // Thêm danh sách cho bộ lọc
        model.addAttribute("categories", categories);
        model.addAttribute("sizes", sizes);

        // Thêm thông tin hiển thị
        long totalElements = productPage.getTotalElements();
        int numberOfElements = productPage.getNumberOfElements();
        int startItem = 0;
        int endItem = 0;
        if (totalElements > 0 && numberOfElements > 0) {
            startItem = page * pageSize + 1;
            endItem = startItem + numberOfElements - 1;
        }
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);

        return "list";
    }

    // Kiểm tra xem có tham số tìm kiếm hoặc lọc nào không
    private boolean hasSearchOrFilter(String keyword, String category, Double minPrice, 
                                    Double maxPrice, String size, Boolean isNew, Boolean isSale) {
        return (keyword != null && !keyword.trim().isEmpty()) ||
               (category != null && !category.trim().isEmpty()) ||
               minPrice != null || maxPrice != null ||
               (size != null && !size.trim().isEmpty()) ||
               isNew != null || isSale != null;
    }
}
