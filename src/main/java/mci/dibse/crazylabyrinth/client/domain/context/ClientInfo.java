package mci.dibse.crazylabyrinth.client.domain.context;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

//TODO notwendig?
@Component
public class ClientInfo {

    @Getter @Setter
    private String playerName;
    @Getter @Setter
    private String identifierToken;
    @Getter @Setter
    private String gameServerName;
    @Getter @Setter
    private boolean isAiControlled;
    @Getter @Setter
    private boolean isAdmin;

}
