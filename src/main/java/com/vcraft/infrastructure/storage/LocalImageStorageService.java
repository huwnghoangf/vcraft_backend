package com.vcraft.infrastructure.storage;

import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.service.ImageStorageService;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalImageStorageService implements ImageStorageService {
    private final Path root;
    private final String baseUrl;

    public LocalImageStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir,
                                   @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public String storeImage(InputStream stream, String filename, String category) {
        String subdir = category == null ? "general" : category;
        if (!subdir.matches("[a-zA-Z0-9_-]+")) throw new InvalidInputException("Thư mục ảnh không hợp lệ");
        try (var imageStream = ImageIO.createImageInputStream(stream)) {
            var readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) throw new InvalidInputException("File không phải ảnh được hỗ trợ");
            var reader = readers.next();
            try {
                reader.setInput(imageStream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 40_000_000)
                    throw new InvalidInputException("Ảnh vượt quá giới hạn 40 megapixel");
                var image = reader.read(0);
                Path dir = root.resolve(subdir);
                Files.createDirectories(dir);
                String name = UUID.randomUUID() + ".jpg";
                Thumbnails.of(image).size(1200, 1200).outputFormat("jpg").outputQuality(0.85)
                        .toFile(dir.resolve(name).toFile());
                return baseUrl + "/uploads/" + subdir + "/" + name;
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new InvalidInputException("Không thể đọc hoặc lưu ảnh");
        }
    }

    @Override
    public void deleteImage(String url) {
        String prefix = baseUrl + "/uploads/";
        if (url == null || !url.startsWith(prefix)) return;
        Path target = root.resolve(url.substring(prefix.length())).normalize();
        if (!target.startsWith(root)) throw new InvalidInputException("Đường dẫn ảnh không hợp lệ");
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể xóa ảnh", ex);
        }
    }
}
