package org.codeNbug.mainserver.external.image.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.codeNbug.mainserver.external.image.dto.ImageUploadRequest;
import org.codeNbug.mainserver.external.image.dto.ImageUploadResponse;
import org.codeNbug.mainserver.external.image.service.S3PresignedUrlService;
import org.codeNbug.mainserver.global.dto.RsData;
import org.codenbug.logging.ControllerLogging;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.security.annotation.RoleRequired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@ControllerLogging
public class ImageUploadController {
    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * presigned URL 배열 발급 (단일 or 다중 모두 배열로)
     */
    @RoleRequired({UserRole.MANAGER, UserRole.ADMIN})
    @PostMapping("/url")
    public ResponseEntity<RsData<List<ImageUploadResponse>>> getPresignedUrls(
            @RequestBody ImageUploadRequest request
    ) {
        List<ImageUploadResponse> responses = request.getFileNames().stream()
                .map(fileName -> new ImageUploadResponse(
                        fileName,
                        s3PresignedUrlService.generatePresignedUploadUrl(fileName, 10)
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new RsData<>(
                "200",
                "presigned URL 발급 성공",
                responses
        ));
    }
}
