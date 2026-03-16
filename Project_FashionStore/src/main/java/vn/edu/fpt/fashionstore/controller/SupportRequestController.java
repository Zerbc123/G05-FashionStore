package vn.edu.fpt.fashionstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.entity.SupportRequest;
import vn.edu.fpt.fashionstore.service.SupportRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportRequestController {

    private final SupportRequestService service;

    @PostMapping
    public SupportRequest create(@RequestBody SupportRequest request) {
        service.create(request);
        return request;
    }

    @GetMapping
    public List<SupportRequest> getAll() {
        return service.getAllRequests();
    }
}