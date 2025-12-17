package mci.dibse.crazylabyrinth.client.domain.model.management;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ErrorResponse {
    private String message;
    private Integer code;
    private Instant timestamp;
}

