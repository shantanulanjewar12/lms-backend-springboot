package com.lms.course.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lms.shared.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceImplTest {

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private S3StorageServiceImpl s3StorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3StorageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3StorageService, "region", "ap-south-1");
    }

    @Test
    void uploadFile_shouldUploadSuccessfullyAndReturnUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my file.pdf",
                "application/pdf",
                "dummy content".getBytes()
        );

        String result = s3StorageService.uploadFile(file, "courses");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3).putObject(captor.capture());

        PutObjectRequest request = captor.getValue();

        assertEquals("test-bucket", request.getBucketName());
        assertEquals(true, request.getKey().startsWith("courses/"));
        assertEquals(true, request.getKey().contains("my_file.pdf"));
        assertEquals("https://test-bucket.s3.ap-south-1.amazonaws.com/" + request.getKey(), result);
    }

    @Test
    void uploadFile_shouldUseDefaultName_whenOriginalFilenameIsNull() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        try {
            when(file.getOriginalFilename()).thenReturn(null);
            when(file.getSize()).thenReturn(20L);
            when(file.getContentType()).thenReturn("text/plain");
            when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("abc".getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String result = s3StorageService.uploadFile(file, "docs");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3).putObject(captor.capture());

        PutObjectRequest request = captor.getValue();

        assertEquals(true, request.getKey().startsWith("docs/"));
        assertEquals(true, request.getKey().contains("_file"));
        assertEquals("https://test-bucket.s3.ap-south-1.amazonaws.com/" + request.getKey(), result);
    }

    @Test
    void uploadFile_shouldThrowBadRequestException_whenInputStreamFails() throws IOException {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        when(file.getOriginalFilename()).thenReturn("video.mp4");
        when(file.getSize()).thenReturn(100L);
        when(file.getContentType()).thenReturn("video/mp4");
        when(file.getInputStream()).thenThrow(new IOException("stream error"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> s3StorageService.uploadFile(file, "lessons")
        );

        assertEquals("Failed to upload file: stream error", exception.getMessage());
        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    void getAccessibleFileUrl_shouldReturnNull_whenFileUrlIsNull() {
        String result = s3StorageService.getAccessibleFileUrl(null);

        assertEquals(null, result);
        verify(amazonS3, never()).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }

    @Test
    void getAccessibleFileUrl_shouldReturnBlank_whenFileUrlIsBlank() {
        String result = s3StorageService.getAccessibleFileUrl("   ");

        assertEquals("   ", result);
        verify(amazonS3, never()).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }

    @Test
    void getAccessibleFileUrl_shouldReturnPresignedUrl_whenValidUrlPassed() throws Exception {
        String fileUrl = "https://test-bucket.s3.ap-south-1.amazonaws.com/courses/java.pdf";
        URL presignedUrl = new URL("https://signed-url.com/temp-access");

        when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(presignedUrl);

        String result = s3StorageService.getAccessibleFileUrl(fileUrl);

        assertEquals("https://signed-url.com/temp-access", result);

        ArgumentCaptor<GeneratePresignedUrlRequest> captor =
                ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(amazonS3).generatePresignedUrl(captor.capture());

        GeneratePresignedUrlRequest request = captor.getValue();
        assertEquals("test-bucket", request.getBucketName());
        assertEquals("courses/java.pdf", request.getKey());
    }

    @Test
    void getAccessibleFileUrl_shouldReturnOriginalUrl_whenPresignedGenerationFails() {
        String fileUrl = "https://test-bucket.s3.ap-south-1.amazonaws.com/courses/java.pdf";

        when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenThrow(new RuntimeException("S3 error"));

        String result = s3StorageService.getAccessibleFileUrl(fileUrl);

        assertEquals(fileUrl, result);
    }

    @Test
    void getAccessibleFileUrl_shouldReturnOriginalUrl_whenKeyCannotBeExtracted() {
        String fileUrl = "https://test-bucket.s3.ap-south-1.amazonaws.com/";

        String result = s3StorageService.getAccessibleFileUrl(fileUrl);

        assertEquals(fileUrl, result);
        verify(amazonS3, never()).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }

    @Test
    void deleteFile_shouldDoNothing_whenFileUrlIsNull() {
        assertDoesNotThrow(() -> s3StorageService.deleteFile(null));

        verify(amazonS3, never()).deleteObject(any(String.class), any(String.class));
    }

    @Test
    void deleteFile_shouldDoNothing_whenFileUrlIsBlank() {
        assertDoesNotThrow(() -> s3StorageService.deleteFile("   "));

        verify(amazonS3, never()).deleteObject(any(String.class), any(String.class));
    }

    @Test
    void deleteFile_shouldDeleteObject_whenValidUrlPassed() {
        String fileUrl = "https://test-bucket.s3.ap-south-1.amazonaws.com/thumbnails/image.png";

        s3StorageService.deleteFile(fileUrl);

        verify(amazonS3).deleteObject("test-bucket", "thumbnails/image.png");
    }

    @Test
    void deleteFile_shouldDoNothing_whenKeyCannotBeExtracted() {
        String fileUrl = "https://test-bucket.s3.ap-south-1.amazonaws.com/";

        assertDoesNotThrow(() -> s3StorageService.deleteFile(fileUrl));

        verify(amazonS3, never()).deleteObject(any(String.class), any(String.class));
    }

    @Test
    void getAccessibleFileUrl_shouldHandleBucketPresentInPath() throws Exception {
        String fileUrl = "https://s3.ap-south-1.amazonaws.com/test-bucket/courses/test%20file.pdf";
        URL presignedUrl = new URL("https://signed-url.com/decoded");

        when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(presignedUrl);

        String result = s3StorageService.getAccessibleFileUrl(fileUrl);

        assertEquals("https://signed-url.com/decoded", result);

        ArgumentCaptor<GeneratePresignedUrlRequest> captor =
                ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(amazonS3).generatePresignedUrl(captor.capture());

        GeneratePresignedUrlRequest request = captor.getValue();
        assertEquals("test-bucket", request.getBucketName());
        assertEquals("courses/test file.pdf", request.getKey());
    }
}