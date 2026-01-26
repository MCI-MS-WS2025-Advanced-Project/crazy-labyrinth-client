package mci.dibse.crazylabyrinth.client.domain.model.management;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private String message;
    private Integer code;
    private Instant timestamp;
}
