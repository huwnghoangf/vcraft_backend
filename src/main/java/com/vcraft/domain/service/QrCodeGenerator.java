package com.vcraft.domain.service;

public interface QrCodeGenerator {
    String generateQrCode(String content, String fileName);
}
