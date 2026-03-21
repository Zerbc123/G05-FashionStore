package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.fashionstore.service.ImageUploadService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class ImageUploadController {

    @Autowired
    private ImageUploadService imageUploadService;

    @PostMapping("/product-image")
    public ResponseEntity<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Validate file
            if (!imageUploadService.isValidImageFile(file)) {
                response.put("error", "Invalid file type. Please upload a valid image file (JPEG, PNG, GIF, WebP)");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Upload to Cloudinary
            String imageUrl = imageUploadService.uploadImage(file);
            
            response.put("success", "Image uploaded successfully");
            response.put("url", imageUrl);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
