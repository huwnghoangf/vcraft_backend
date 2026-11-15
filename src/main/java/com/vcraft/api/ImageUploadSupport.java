package com.vcraft.api;

import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.service.ImageStorageService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** Compensate filesystem writes if a multi-image request fails part way through. */
public final class ImageUploadSupport {
    private ImageUploadSupport() {}
    public static List<String> storeAll(ImageStorageService storage, List<MultipartFile> files, String category) {
        if (files == null || files.isEmpty()) throw new InvalidInputException("Cần ít nhất một ảnh");
        var urls = new ArrayList<String>();
        try {
            for (var file : files) {
                try (var stream = file.getInputStream()) {
                    urls.add(storage.storeImage(stream, file.getOriginalFilename(), category));
                }
            }
            return urls;
        } catch (Exception ex) {
            cleanup(storage, urls, ex);
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new InvalidInputException("Không thể đọc ảnh tải lên");
        }
    }
    public static void cleanup(ImageStorageService storage, List<String> urls, Exception failure) {
        for (var url : urls) {
            try { storage.deleteImage(url); } catch (RuntimeException cleanupError) { failure.addSuppressed(cleanupError); }
        }
    }
}
