package com.na.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 *  403（禁止访问）
 */
public class NaForbiddenException extends RuntimeException {
    private static final long serialVersionUID = -3804995326646218863L;

    private HttpStatus status;

    public NaForbiddenException(String ex, HttpStatus status) {
        super(ex);
        if (status != null) {
            this.status = status;
        } else {
            this.status = HttpStatus.OK;
        }

    }

    public HttpStatus getStatus() {
        return status;
    }
}