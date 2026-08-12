package com.designmd.designapi.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(1111, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_MESSAGE(1002, "Invalid message key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1001, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1001, "User existed", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(1003, "User must be at least {min} character", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least {min} character", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    ANALYSIS_NOT_FOUND(2001, "Analysis not found", HttpStatus.BAD_REQUEST),
    ANALYSIS_CANNOT_BE_DELETED(2002, "Running analysis cannot be deleted", HttpStatus.CONFLICT),
    ANALYSIS_CRAWL_FAILED(
            2003,
            "Analysis crawl failed",
            HttpStatus.UNPROCESSABLE_ENTITY),

    ANALYSIS_NOT_COMPLETED(
            2004,
            "Analysis has not completed crawling",
            HttpStatus.CONFLICT),

    ANALYSIS_RESULT_NOT_READY(
            2005,
            "AI analysis result is not ready",
            HttpStatus.CONFLICT
    );

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;

}
