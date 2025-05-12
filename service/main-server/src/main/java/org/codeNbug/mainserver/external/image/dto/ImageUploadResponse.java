package org.codeNbug.mainserver.external.image.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageUploadResponse {
    private String fileName;
    private String url;
}
