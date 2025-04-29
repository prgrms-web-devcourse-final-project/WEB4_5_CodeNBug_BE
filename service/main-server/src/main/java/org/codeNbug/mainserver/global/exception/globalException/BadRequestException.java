package org.codeNbug.mainserver.global.exception.globalException;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
