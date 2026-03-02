package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.repository.OrderRepository;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;

    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role);
    }

    @GetMapping
    public String viewOrders(HttpSession session, Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Order> orders = orderRepository.findAll();

        long pending = orders.stream()
                .filter(o -> "Pending".equals(o.getStatus()))
                .count();

        long shipping = orders.stream()
                .filter(o -> "Shipping".equals(o.getStatus()))
                .count();

        long completed = orders.stream()
                .filter(o -> "Completed".equals(o.getStatus()))
                .count();

        double revenue = orders.stream()
                .filter(o -> "Completed".equals(o.getStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        model.addAttribute("orders", orders);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("shippingCount", shipping);
        model.addAttribute("completedCount", completed);
        model.addAttribute("totalRevenue", revenue);

        return "admin/adminorder";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id,
            @RequestParam String status) {

        orderRepository.findById(id).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });

        return "redirect:/admin/orders";
    }

    @GetMapping("/orderdetails/{id}")
    public String viewOrderDetails(@PathVariable Long id, Model model) {

        Order order = orderRepository.findOrderWithItems(id);
        model.addAttribute("order", order);

        return "admin/vieworderdetail";
    }
}