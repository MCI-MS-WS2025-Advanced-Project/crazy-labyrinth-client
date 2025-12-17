package mci.dibse.crazylabyrinth.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.concurrent.CompletionException;
import labyrinth.contracts.models.CommandType;
import labyrinth.contracts.models.ConnectCommandPayload;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.domain.model.ClientPlayer;
import mci.dibse.crazylabyrinth.client.domain.model.record.GameInfoRecord;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.ConnectionException;
import mci.dibse.crazylabyrinth.client.infrastructure.gameserver.GameServerSocketClient;
import mci.dibse.crazylabyrinth.client.infrastructure.util.CommandUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameServerSocketService implements PlayerConnectedAckListener {

    private final ApplicationEventPublisher eventPublisher;
    private final GameServerSocketClient client;

    private final LobbyService lobbyService;

    private String serverUrl;
    private String identifierToken;
    private String clientUserName;

    @Autowired
    public GameServerSocketService(
            GameServerSocketClient client,
            ApplicationEventPublisher eventPublisher,
            LobbyService lobbyService) {
        this.client = client;
        this.eventPublisher = eventPublisher;
        this.lobbyService = lobbyService;
        lobbyService.addPlayerListener(this);
    }

    public String sendCommand(String command) {
        client.sendMessage(command);
        return "Command sent";
    }

    public void connectToServerAndSendMessage(String serverUrl, String json)
            throws ConnectionException {
        try {
            client.connect(serverUrl.trim());
            // Wait for the connection to complete; blocks current thread
            client.getConnectionCallback().join();

            client.sendMessage(json);
        } catch (CompletionException ex) {
            throw new ConnectionException("Connection to server failed", ex.getCause());
        }
        this.serverUrl = serverUrl.trim();
    }

    @EventListener
    public void handleDisconnectEvent(ApplicationEvents event) {
        // todo remove after test
        System.out.println(
                "Disconnect event received "
                        + event
                        + " "
                        + (serverUrl)
                        + "  "
                        + (identifierToken)
                        + "   "
                        + (clientUserName));
        if (ApplicationEvents.CONNECTION_LOST.equals(event)
                && (serverUrl != null && identifierToken != null && clientUserName != null)) {
            // todo remove after test
            System.out.println("trying to connect to server");
            ConnectCommandPayload reconnectPayload = new ConnectCommandPayload();
            reconnectPayload.setIdentifierToken(identifierToken);
            reconnectPayload.setType(CommandType.CONNECT);
            reconnectPayload.setUsername(this.clientUserName);
            try {
                String reconnectJson =
                        CommandUtils.mapCommandToMessage(CommandType.CONNECT, reconnectPayload);
                client.reconnectToServerWithToken(serverUrl, reconnectJson);
            } catch (JsonProcessingException e) {
                log.error("Error serializing reconnect payload", e);
            }
        }
    }

    @EventListener
    public void saveGameInfoData(GameInfoRecord gameInfo) {
        this.clientUserName = gameInfo.playerName();
    }

    @EventListener
    public void handlePlayerDrivenDisconnect(CommandType command) {
        if (CommandType.DISCONNECT.equals(command)) {
            log.debug("Handle player disconnect event");
            this.identifierToken = null;
            this.clientUserName = null;
            this.serverUrl = null;
        }
    }

    @EventListener
    public void handleBackToServerDashboard(ApplicationEvents event) {
        if (ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD.equals(event)) {
            log.debug("Handle Back-to-Lobby event");
            this.identifierToken = null;
            this.serverUrl = null;
            this.clientUserName = null;
        }
    }

    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        log.debug("Handle onPlayerConnected event");
        this.identifierToken = clientPlayer.getIdentifierToken();
    }
}
