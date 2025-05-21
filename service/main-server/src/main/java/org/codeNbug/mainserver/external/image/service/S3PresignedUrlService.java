package org.codeNbug.mainserver.external.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {
    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region.static}")
    private String region;


    private final S3Client s3Client;
    private final StaticCredentialsProvider credentialsProvider;

    public String generatePresignedUploadUrl(String fileName, int expirationMinutes) {
        String contentType = guessContentType(fileName); // 확장자 기반 추출

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(expirationMinutes))
                        .putObjectRequest(objectRequest)
                        .build()
        );

        return presignedRequest.url().toString();
    }


    private String extractExtension(String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length < 2) return ""; // 확장자 없음
        return parts[parts.length - 1].toLowerCase(); // 마지막 요소가 확장자
    }

    private String guessContentType(String fileName) {
        String ext = extractExtension(fileName);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream"; // fallback
        };
    }

}
