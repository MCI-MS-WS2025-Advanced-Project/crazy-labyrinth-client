package mci.dibse.crazylabyrinth.client.infrastructure.exceptions;

import lombok.Getter;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.management.ErrorResponse;

public class GeneralUiException extends RuntimeException {

    @Getter @Setter private ErrorResponse errorResponse;

    public GeneralUiException(String message) {
        super(message);
    }

    public GeneralUiException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneralUiException(String message, Throwable cause, ErrorResponse errorResponseBody) {
        super(message, cause);
        this.errorResponse = errorResponseBody;
    }

    public GeneralUiException(String message, ErrorResponse errorResponseBody) {
        super(message);
        this.errorResponse = errorResponseBody;
    }
}
