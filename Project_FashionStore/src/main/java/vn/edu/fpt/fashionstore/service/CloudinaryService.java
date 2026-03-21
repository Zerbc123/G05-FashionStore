package vn.edu.fpt.fashionstore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Debug: Check Cloudinary config
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", "fashion_store/products",
            "resource_type", "image"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        return uploadResult.get("url").toString();
    }

    public String generateImageUrl(String publicId) {
        return cloudinary.url()
            .secure(true)
            .format("auto")
            .generate(publicId);
    }

    public String generateTransformedUrl(String publicId, int width, int height) {
        return cloudinary.url()
            .secure(true)
            .format("auto")
            .transformation(
                new Transformation()
                    .width(width)
                    .height(height)
                    .crop("fill")
                    .gravity("auto")
            )
            .generate(publicId);
    }

    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
