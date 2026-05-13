package com.hawkstack.api.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class S3Service {
    public String upload(MultipartFile file, String key) {
        throw new UnsupportedOperationException("S3 not configured");
    }
}
