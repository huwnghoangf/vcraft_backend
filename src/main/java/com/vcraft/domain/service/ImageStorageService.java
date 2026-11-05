package com.vcraft.domain.service;

import java.io.InputStream;
import java.util.List;

public interface ImageStorageService {
    String storeImage(InputStream inputStream, String originalFilename, String category);
    void deleteImage(String fileUrl);
}
