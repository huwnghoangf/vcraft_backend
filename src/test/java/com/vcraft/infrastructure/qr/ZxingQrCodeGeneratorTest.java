package com.vcraft.infrastructure.qr;

import static org.assertj.core.api.Assertions.assertThat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZxingQrCodeGeneratorTest {
    @TempDir Path dir;
    @Test void generatedPngDecodesToExactTraceUrl() throws Exception {
        String target = "http://localhost:8080/trace/12345678-1234-1234-1234-123456789abc";
        var generator = new ZxingQrCodeGenerator(dir.toString(), "http://localhost:8080");
        assertThat(generator.generateQrCode(target, "batch")).isEqualTo("http://localhost:8080/uploads/qr/batch.png");
        var image = ImageIO.read(dir.resolve("qr/batch.png").toFile());
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(target);
    }
}
