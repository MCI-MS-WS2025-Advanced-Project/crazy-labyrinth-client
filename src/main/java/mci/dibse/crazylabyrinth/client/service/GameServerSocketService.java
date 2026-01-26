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
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GeneralUiException;
import mci.dibse.crazylabyrinth.client.infrastructure.gameserver.GameServerSocketClient;
import mci.dibse.crazylabyrinth.client.infrastructure.util.CommandUtils;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
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
    private final NavigationStateViewModel navState;

    private String serverUrl;
    private String identifierToken;
    private String clientUserName;

    @Autowired
    public GameServerSocketService(
            GameServerSocketClient client,
            ApplicationEventPublisher eventPublisher,
            LobbyService lobbyService,
            NavigationStateViewModel navState) {
        this.client = client;
        this.eventPublisher = eventPublisher;
        this.lobbyService = lobbyService;
        this.navState = navState;
        lobbyService.addPlayerListener(this);
    }

    /**
     * Send a command to the game server and publish the command event locally.
     *
     * @param command serialized command payload
     * @param commandType command type to publish locally
     * @throws GeneralUiException when the socket send fails
     */
    public void sendCommand(String command, CommandType commandType) throws GeneralUiException {
        try {
            client.sendMessage(command);
        } catch (ConnectionException e) {
            log.error("Could not send command", e);
            throw new GeneralUiException("Could not send command");
        }
        if (commandType == null) return;
        SendCommand sendCommand = new SendCommand(commandType.name(), commandType);
        eventPublisher.publishEvent(sendCommand);
    }

    /**
     * Connect to the game server and send an initial message.
     *
     * @param serverUrl server URL to connect to
     * @param json initial message payload
     * @throws ConnectionException when the connection fails
     */
    public void connectToServerAndSendMessage(String serverUrl, String json)
            throws ConnectionException {
        try {
            client.connect(serverUrl.trim(), null);
            // Wait for the connection to complete; blocks current thread
            client.getConnectionCallback().join();

            client.sendMessage(json);
        } catch (CompletionException ex) {
            throw new ConnectionException("Connection to server failed", ex.getCause());
        }
        this.serverUrl = serverUrl.trim();
    }

    /**
     * Configure whether incoming messages are blocked in the socket client.
     *
     * @param canReceive true to allow receiving messages; false to block
     */
    public void setBlockForReceivingMessages(boolean canReceive) {
        client.setBlockForReceivingMessages(canReceive);
    }

    /**
     * Attempt to reconnect using saved connection information when a disconnect event occurs.
     *
     * @param event application event signalling a disconnect
     */
    @EventListener
    public void handleDisconnectEvent(ApplicationEvents event) {
        if (ApplicationEvents.CONNECTION_LOST.equals(event)
                && (serverUrl != null && identifierToken != null && clientUserName != null)) {

            log.info("Disconnect from server event detected");

            ConnectCommandPayload reconnectPayload = new ConnectCommandPayload();

            if (!NavigationStateViewModel.ScreenId.LOBBY.equals(navState.getCurrentScreenId())) {
                reconnectPayload.setIdentifierToken(identifierToken);
            }

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

    /**
     * Store the player name from the latest game info record.
     *
     * @param gameInfo game info payload
     */
    @EventListener
    public void saveGameInfoData(GameInfoRecord gameInfo) {
        this.clientUserName = gameInfo.playerName();
    }

    /**
     * Clear connection state when the player initiates a disconnect.
     *
     * @param command disconnect command type
     */
    @EventListener
    public void handlePlayerDrivenDisconnect(CommandType command) {
        if (CommandType.DISCONNECT.equals(command)) {
            log.debug("Handle player disconnect event");
            this.identifierToken = null;
            this.clientUserName = null;
            this.serverUrl = null;
        }
    }

    /**
     * Reset connection state when returning to the server dashboard.
     *
     * @param event application event signalling navigation change
     */
    @EventListener
    public void handleBackToServerDashboard(ApplicationEvents event) {
        if (ApplicationEvents.BACK_TO_GAME_SERVER_DASHBOARD.equals(event)) {
            log.debug("Handle Back-to-Server Dashboard event");
            this.identifierToken = null;
            this.serverUrl = null;
            this.clientUserName = null;
        }
    }

    /**
     * Capture the identifier token from the connected player payload.
     *
     * @param clientPlayer connected player payload
     */
    @Override
    public void onPlayerConnected(ClientPlayer clientPlayer) {
        log.debug("Handle onPlayerConnected event");
        this.identifierToken = clientPlayer.getIdentifierToken();
    }

    public record SendCommand(String title, CommandType commandType) {}
}
