package com.vcraft.infrastructure.storage;

import com.vcraft.domain.service.ImageStorageService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalImageStorageService implements ImageStorageService {

    private final String uploadDir;
    private final String baseUrl;

    public LocalImageStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir,
                                    @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
    }

    @Override
    public String storeImage(InputStream inputStream, String originalFilename, String category) {
        try {
            String subDir = category != null ? category : "general";
            Path targetDir = Paths.get(uploadDir, subDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String ext = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            String uniqueName = UUID.randomUUID().toString() + ext;
            Path targetFile = targetDir.resolve(uniqueName);

            // Nén và tối ưu hóa kích cỡ ảnh (Max 1200x1200px)
            Thumbnails.of(inputStream)
                    .size(1200, 1200)
                    .outputQuality(0.85)
                    .toFile(targetFile.toFile());

            return baseUrl + "/uploads/" + subDir + "/" + uniqueName;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu trữ hình ảnh: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.contains("/uploads/")) {
                String relativePath = fileUrl.substring(fileUrl.indexOf("/uploads/") + 9);
                Path path = Paths.get(uploadDir, relativePath);
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {}
    }
}
