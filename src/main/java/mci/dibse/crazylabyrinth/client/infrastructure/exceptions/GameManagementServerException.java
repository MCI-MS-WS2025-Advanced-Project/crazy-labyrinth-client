package mci.dibse.crazylabyrinth.client.infrastructure.exceptions;


/**
 *
 *  @author Julian Janetschek
 */

public class GameManagementServerException extends Exception {
    public GameManagementServerException() {}

    public GameManagementServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameManagementServerException(String message) {
        super(message);
    }

    public GameManagementServerException(Exception e) {
        super(e);
    }
}
