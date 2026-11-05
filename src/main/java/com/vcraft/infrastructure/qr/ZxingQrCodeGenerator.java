package com.vcraft.infrastructure.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vcraft.domain.service.QrCodeGenerator;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ZxingQrCodeGenerator implements QrCodeGenerator {

    private final String uploadDir;
    private final String baseUrl;

    public ZxingQrCodeGenerator(@Value("${app.upload-dir:./uploads}") String uploadDir,
                                @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
    }

    @Override
    public String generateQrCode(String content, String fileName) {
        try {
            Path qrDir = Paths.get(uploadDir, "qr");
            if (!Files.exists(qrDir)) {
                Files.createDirectories(qrDir);
            }

            String fileWithExt = fileName.endsWith(".png") ? fileName : fileName + ".png";
            Path targetFile = qrDir.resolve(fileWithExt);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 350, 350);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", targetFile);

            return baseUrl + "/uploads/qr/" + fileWithExt;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh mã QR: " + e.getMessage(), e);
        }
    }
}
