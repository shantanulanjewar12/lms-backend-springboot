package com.lms.course.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lms.course.service.S3StorageService;
import com.lms.shared.exception.BadRequestException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

	private final AmazonS3 amazonS3;

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Value("${aws.region}")
	private String region;

	@Override
	public String uploadFile(MultipartFile file, String folder) {
		String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
		String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
		String fileName = folder + "/" + UUID.randomUUID() + "_" + safeName;
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.getSize());
		metadata.setContentType(file.getContentType());
		try {
			amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
		} catch (IOException e) {
			throw new BadRequestException("Failed to upload file: " + e.getMessage());
		}
		return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;
	}

	@Override
	public String getAccessibleFileUrl(String fileUrl) {
		if (fileUrl == null || fileUrl.isBlank()) {
			return fileUrl;
		}

		String key = extractKey(fileUrl);
		if (key == null || key.isBlank()) {
			return fileUrl;
		}

		try {
			Date expiry = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 7));
			GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key)
					.withExpiration(expiry);
			return amazonS3.generatePresignedUrl(request).toString();
		} catch (Exception ex) {
			return fileUrl;
		}
	}

	@Override
	public void deleteFile(String fileUrl) {
		if (fileUrl == null || fileUrl.isBlank())
			return;
		String key = extractKey(fileUrl);
		if (key == null || key.isBlank()) {
			return;
		}
		amazonS3.deleteObject(bucketName, key);
	}

	private String extractKey(String fileUrl) {
		try {
			URI uri = URI.create(fileUrl);
			String path = uri.getPath();
			if (path == null || path.isBlank()) {
				return null;
			}
			String normalized = path.startsWith("/") ? path.substring(1) : path;
			if (normalized.startsWith(bucketName + "/")) {
				normalized = normalized.substring(bucketName.length() + 1);
			}
			return URLDecoder.decode(normalized, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			return null;
		}
	}
}