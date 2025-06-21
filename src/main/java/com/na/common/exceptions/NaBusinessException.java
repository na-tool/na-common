package com.na.common.exceptions;

import com.na.common.result.enums.NaStatus;
import org.springframework.http.HttpStatus;


public class NaBusinessException extends RuntimeException {
    private static final long serialVersionUID = -3804995326646218863L;

    private HttpStatus httpStatus;

    public NaBusinessException(String ex, HttpStatus httpStatus) {
        super(ex);
        this.httpStatus = resolveHttpStatus(httpStatus);
    }

    public NaBusinessException(NaStatus status, HttpStatus httpStatus) {
        super(status.getMsg());
        this.httpStatus = resolveHttpStatus(httpStatus);
    }

    public HttpStatus getStatus() {
        return httpStatus;
    }

    private HttpStatus resolveHttpStatus(HttpStatus status) {
        return status != null ? status : HttpStatus.OK;
    }
}
