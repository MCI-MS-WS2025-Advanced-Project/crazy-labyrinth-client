package mci.dibse.crazylabyrinth.client.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import labyrinth.contracts.models.ServerInfoPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TimerService {

    private Duration clockOffset = Duration.ZERO;

    public void handleGameServerTimeInfo(ServerInfoPayload serverInfo) {
        OffsetDateTime localTime = OffsetDateTime.now();
        OffsetDateTime serverTime = serverInfo.getServerTime();

        clockOffset = Duration.between(localTime, serverTime);

        if (clockOffset.isNegative()) {
            log.info("Server is behind client time by " + clockOffset.abs());
        } else {
            log.info("Server is ahead client time by " + clockOffset);
        }
    }

    public OffsetDateTime getCurrentServerTimeWithGameServerOffset() {
        return OffsetDateTime.now().plus(clockOffset);
    }
}
