package mci.dibse.crazylabyrinth.client.infrastructure.exceptions;

import lombok.Getter;
import lombok.Setter;

public class GameManagementServerException extends Exception {

    @Getter @Setter String errorResponseBody;

    public GameManagementServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameManagementServerException(
            String message, Throwable cause, String errorResponseBody) {
        super(message, cause);
        this.errorResponseBody = errorResponseBody;
    }

    public GameManagementServerException(String message) {
        super(message);
    }

    public GameManagementServerException(Exception e) {
        super(e);
    }
}
