package vn.edu.fpt.fashionstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.service.CategoryService;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", service.getAllCategories());
        return "admin/category-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Category category) {
        service.save(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable int id, Model model) {
        model.addAttribute("category", service.getById(id));
        return "admin/category-form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable int id, Model model) {

        boolean deleted = service.delete(id);

        if (!deleted) {
            model.addAttribute("error",
                    "Cannot delete category because it is used by a product.");
        }

        model.addAttribute("categories", service.getAllCategories());

        return "admin/category-list";
    }
}