package mci.dibse.crazylabyrinth.client.infrastructure.gameserver;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import labyrinth.contracts.models.ActionErrorEventPayload;
import labyrinth.contracts.models.CommandType;
import labyrinth.contracts.models.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.ConnectionException;
import mci.dibse.crazylabyrinth.client.service.ServerEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Component
@Slf4j
public class GameServerSocketClient {

    private final AtomicBoolean reconnectingFlag = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);

    private static final String SOCKET_GAME_PATH = "/game";

    private static final int RETRY_DELAY_SECONDS = 1;
    private static final int CONNECT_TIMEOUT_SECONDS = 5;

    private static final String WARNING_LOST_CONNECTION =
            "Cannot send message: WebSocket is not connected. Scheduling retry in {}s";
    private static final Duration RECONNECT_BASE_DELAY = Duration.ofSeconds(1);
    private static final Duration RECONNECT_MAX_DELAY = Duration.ofSeconds(10);

    private final ClientWebSocketHandler webSocketHandler;

    private final ServerEventService serverEventService;

    private WebSocketClient webSocketClient;

    private final ScheduledExecutorService reconnectScheduler =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "ws-reconnect");
                        t.setDaemon(true);
                        return t;
                    });

    /*
     What WebSocketConnectionManager does do
           1. **Connection Lifecycle**: Handles starting/stopping the connection as part of Spring's lifecycle
           2. **Integration Layer**: Bridges Spring's lifecycle management with WebSocket clients
           3. **Resource Management**: Manages the underlying WebSocket client resources
      Does NOT Do:
           1. **No Auto-Reconnection**: It doesn't automatically reconnect when connections are lost
           2. **No Error Recovery**: It doesn't handle transport errors or connection failures beyond logging them
           3. **No Connection Monitoring**: It doesn't actively monitor connection health
    */
    private WebSocketConnectionManager connectionManager;

    private volatile boolean shouldReconnect = false;
    private CompletableFuture<Void> connectionFuture;

    @Autowired
    public GameServerSocketClient(
            ClientWebSocketHandler webSocketHandler, ServerEventService serverEventService) {
        this.webSocketHandler = webSocketHandler;
        this.serverEventService = serverEventService;
    }

    /**
     * Send a message to the game server over the active WebSocket session.
     *
     * @param message message payload
     * @throws ConnectionException when no open session is available
     */
    public void sendMessage(String message) throws ConnectionException {

        try {
            WebSocketSession session = findOpenSession();

            session.sendMessage(new TextMessage(message));
            log.debug("Message sent successfully: {}", message);

        } catch (IOException e) {
            log.error("IOException", e);
            throw new ConnectionException("IOException", e);
        } catch (IllegalStateException e) {
            log.error("No active WebSocket session found");
            throw new ConnectionException("No active WebSocket session found", e);
        }
    }

    /**
     * Connect to the server and optionally run a callback on success.
     *
     * @param serverUrl server URL
     * @param connectCallback optional callback after connection
     * @throws ConnectionException when connection fails
     */
    public void connect(String serverUrl, Runnable connectCallback) throws ConnectionException {
        log.debug("Connecting to WebSocket server at: {}", serverUrl);

        if (connectionManager != null && connectionManager.isRunning()) {
            log.warn("connectionManager connected. Disconnecting first...");
            disconnect();
        }
        connectionFuture = new CompletableFuture<>();

        try {
            WebSocketClient client = new StandardWebSocketClient();

            // Set up handler to complete future on connection
            webSocketHandler.setConnectionCallback(connectionFuture);

            connectionManager = new WebSocketConnectionManager(client, webSocketHandler, serverUrl);
            connectionManager.setAutoStartup(false);
            connectionManager.start();

            if (connectCallback != null) {
                connectCallback.run();
            } else {
                connectionFuture
                        .orTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS) // Built-in timeout
                        .thenRun(
                                () -> {
                                    markConnected();
                                    log.info("Successfully connected to WebSocket server");
                                })
                        .exceptionally(
                                throwable -> {
                                    log.error(
                                            "Failed to connect to WebSocket server: {}",
                                            throwable.getMessage());
                                    return null;
                                });
            }

        } catch (Exception e) {
            log.error("Failed to connect to WebSocket server: {}", e.getMessage(), e);
            connectionFuture.completeExceptionally(e);
            throw new ConnectionException("Failed to connect to WebSocket server", e);
        }
    }

    /**
     * Schedule a reconnect attempt to the server.
     *
     * @param lastServerUrl last known server URL
     * @param callback callback to run after successful reconnection
     */
    public void reconnect(String lastServerUrl, Runnable callback) {
        if (lastServerUrl == null || lastServerUrl.isBlank()) {
            log.warn("Reconnect requested but no serverUrl known yet (call connect(url) first)");
            return;
        }
        log.debug("Reconnecting to WebSocket server at: {}", lastServerUrl);
        scheduleReconnectAttempt(lastServerUrl, callback);
    }

    /**
     * Stop reconnection attempts when the server reports the player missing.
     *
     * @param error action error payload
     */
    @EventListener
    public void handleReconnectionAbort(ActionErrorEventPayload error) {
        if (ErrorCode.PLAYER_NOT_FOUND.equals(error.getErrorCode())) reconnectingFlag.set(true);
    }

    /**
     * Disconnect when a client disconnect command is issued.
     *
     * @param event disconnect command type
     */
    @EventListener
    public void ensureDisconnect(CommandType event) {
        if (CommandType.DISCONNECT.equals(event)) {
            disconnect();
            shouldReconnect = false;
        }
    }

    /**
     * Close the connection and cleanup
     */
    public void disconnect() {
        try {
            if (connectionManager != null && connectionManager.isRunning()) {
                connectionManager.stop();
                log.info("WebSocket connection manager stopped");
            }

            // Close any remaining sessions
            webSocketHandler.closeSession();
        } catch (Exception e) {
            log.error("Error during disconnect: {}", e.getMessage());
        }
    }

    /**
     * Reconnect to the server using a prebuilt reconnect payload.
     *
     * @param serverUrl server URL
     * @param reconnectPlay reconnect payload JSON
     */
    public void reconnectToServerWithToken(String serverUrl, String reconnectPlay) {
        Runnable callback =
                () -> {
                    sendMessage(reconnectPlay);
                };
        reconnect(serverUrl, callback);
    }

    /**
     * Return the connection completion future.
     *
     * @return connection completion future
     */
    public CompletableFuture<Void> getConnectionCallback() {
        return connectionFuture;
    }

    /**
     * Enable or disable message receiving on the socket handler.
     *
     * @param canReceive true to receive messages
     */
    public void setBlockForReceivingMessages(boolean canReceive) {
        webSocketHandler.setBlockForReceivingMessages(canReceive);
    }

    /**
     * Check whether a reconnect attempt may proceed.
     *
     * @return true when reconnect can proceed
     */
    private boolean canProceedWithReconnecting() {
        boolean ret = reconnectingFlag.compareAndSet(true, false);
        log.debug("reconnectAttemptInProgress : {}", ret);
        return ret;
    }

    /**
     * Reset reconnection progress when another attempt is needed.
     */
    private void resetReconnectionProgress() {
        if (canContinueToReconnect()) reconnectingFlag.set(true);
    }

    /**
     * Check if reconnection should continue.
     *
     * @return true when reconnection should continue
     */
    private boolean canContinueToReconnect() {
        return shouldReconnect;
    }

    /**
     * Mark the connection as established and reset counters.
     */
    private void markConnected() {
        shouldReconnect = true;
        reconnectingFlag.set(true);
        reconnectAttempt.set(0);
    }

    /**
     * Find an open WebSocket session.
     *
     * @return open session
     * @throws IllegalStateException when no open session is found
     */
    private WebSocketSession findOpenSession() throws IllegalStateException {
        return webSocketHandler.getSessions().values().stream()
                .filter(WebSocketSession::isOpen)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No open WebSocket session found"));
    }

    /**
     * Schedule a reconnect attempt with exponential backoff.
     *
     * @param lastServerUrl server URL
     * @param callback callback to run after reconnection
     */
    private void scheduleReconnectAttempt(String lastServerUrl, Runnable callback) {
        if (!canProceedWithReconnecting()) return;
        int attempt = reconnectAttempt.getAndIncrement();
        Duration delay = nextBackoffDelay(attempt);

        Runnable connectionCallback =
                () -> {
                    connectionFuture
                            .orTimeout(reconnectAttempt.get(), TimeUnit.SECONDS)
                            .thenRun(
                                    () -> {
                                        log.info("connection Future completed successfully");
                                        markConnected();
                                        callback.run();
                                    })
                            .exceptionally(
                                    (_) -> {
                                        log.error("connection Future completed exceptionally");
                                        resetReconnectionProgress();
                                        scheduleReconnectAttempt(lastServerUrl, callback);
                                        return null;
                                    });
                };

        reconnectScheduler.schedule(
                () -> {
                    try {
                        log.info("Reconnect attempt #{} (delay {}ms)", attempt, delay.toMillis());
                        connect(lastServerUrl, connectionCallback);
                    } catch (Exception e) {
                        log.error("Reconnect attempt failed", e);
                        resetReconnectionProgress();
                        scheduleReconnectAttempt(lastServerUrl, callback); // keep trying
                    }
                },
                delay.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    /**
     * Compute the next backoff delay for a reconnect attempt.
     *
     * @param attempt attempt number
     * @return backoff delay duration
     */
    private Duration nextBackoffDelay(int attempt) {
        long baseMs = RECONNECT_BASE_DELAY.toMillis();
        long maxMs = RECONNECT_MAX_DELAY.toMillis();

        log.info("nextBackoffDelay -> {}", attempt);

        long expMs = (attempt >= 10) ? maxMs : Math.min(maxMs, baseMs * (1L << attempt));

        return Duration.ofMillis(expMs);
    }
}
