package com.lms.course.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3StorageService {
    String uploadFile(MultipartFile file, String folder);

    String getAccessibleFileUrl(String fileUrl);

    void deleteFile(String fileUrl);
}