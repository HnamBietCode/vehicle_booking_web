package com.bookvehicle.example.sr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${app.upload.dir:uploads/vehicles}")
    private String uploadDir;

    /**
     * Lưu file ảnh vào thư mục upload.
     * @return đường dẫn tương đối để lưu vào DB, ví dụ: "/uploads/vehicles/abc123.jpg"
     */
    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate loại file
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPG, PNG, WebP, GIF).");
        }

        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;

        // Lưu file
        Path targetPath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về URL path để lưu vào DB
        return "/uploads/vehicles/" + newFilename;
    }
}
