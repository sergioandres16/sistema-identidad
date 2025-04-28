package org.saeta.digitalidentitysystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidQrException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidQrException(String message) {
        super(message);
    }
}
