package com.vcraft.infrastructure.storage;

import static org.assertj.core.api.Assertions.*;
import com.vcraft.domain.exception.InvalidInputException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalImageStorageServiceTest {
    @TempDir Path dir;
    @Test void encodesAsJpegRegardlessOfOriginalExtension() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2400, 1400, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var storage = new LocalImageStorageService(dir.toString(), "http://localhost:8080");
        String url = storage.storeImage(new ByteArrayInputStream(bytes.toByteArray()), "camera.webp", "products");
        assertThat(url).endsWith(".jpg");
        Path saved = dir.resolve(url.split("/uploads/")[1]);
        var image = ImageIO.read(saved.toFile());
        assertThat(image.getWidth()).isEqualTo(1200);
        storage.deleteImage(url);
        assertThat(saved).doesNotExist();
    }
    @Test void rejectsNonImagesAndTraversal() {
        var storage = new LocalImageStorageService(dir.toString(), "http://localhost:8080");
        assertThatThrownBy(() -> storage.storeImage(new ByteArrayInputStream("bad".getBytes()), "a.png", "products"))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> storage.storeImage(new ByteArrayInputStream(new byte[0]), "a.png", "../outside"))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> storage.deleteImage("http://localhost:8080/uploads/../../outside"))
                .isInstanceOf(InvalidInputException.class);
    }
}
