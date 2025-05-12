package org.codeNbug.mainserver.external.image.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ImageUploadRequest {
    private List<String> fileNames;
}
