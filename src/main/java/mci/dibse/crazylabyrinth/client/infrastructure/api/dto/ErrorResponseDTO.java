package mci.dibse.crazylabyrinth.client.infrastructure.api.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * modeled after the GameManagementServer_openapi.yml
 * @author Julian Janetschek
 */

@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDTO {

    @NotNull
    private String message;
    private Integer code;
    private Instant timestamp;
}
