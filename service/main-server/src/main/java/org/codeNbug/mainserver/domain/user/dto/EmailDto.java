package org.codeNbug.mainserver.domain.user.dto;

import lombok.Data;

public class EmailDto {
    
    @Data
    public static class SendRequest {
        private String mail;
    }

    @Data
    public static class VerifyRequest {
        private String mail;
        private String verifyCode;
    }
}