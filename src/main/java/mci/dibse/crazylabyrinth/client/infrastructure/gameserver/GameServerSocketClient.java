package mci.dibse.crazylabyrinth.client.infrastructure.gameserver;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.ConnectionException;
import mci.dibse.crazylabyrinth.client.service.ServerEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Component
@Slf4j
public class GameServerSocketClient {

    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);

    private static final String SOCKET_GAME_PATH = "/game";

    private static final int RETRY_DELAY_SECONDS = 1;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;

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

    // todo isConnected needed?
    private volatile boolean isConnected = false;
    private CompletableFuture<Void> connectionFuture;

    @Autowired
    public GameServerSocketClient(
            ClientWebSocketHandler webSocketHandler, ServerEventService serverEventService) {
        this.webSocketHandler = webSocketHandler;
        this.serverEventService = serverEventService;
    }

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

    public void connect(String serverUrl) throws ConnectionException {
        log.debug("Connecting to WebSocket server at: {}", serverUrl);

        if (connectionManager != null && connectionManager.isRunning()) {
            log.warn("Already connected. Disconnecting first...");
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
                                markDisconnected();
                                return null;
                            });

        } catch (Exception e) {
            log.error("Failed to connect to WebSocket server: {}", e.getMessage(), e);
            connectionFuture.completeExceptionally(e);
            markDisconnected();
            throw new ConnectionException("Failed to connect to WebSocket server", e);
        }
    }

    public void reconnect(String lastServerUrl, Runnable callback) {
        if (lastServerUrl == null || lastServerUrl.isBlank()) {
            log.warn("Reconnect requested but no serverUrl known yet (call connect(url) first)");
            return;
        }

        if (!reconnecting.compareAndSet(false, true)) {
            return; // reconnect already running
        }

        scheduleReconnectAttempt(lastServerUrl, callback);
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
            markDisconnected();
        } catch (Exception e) {
            log.error("Error during disconnect: {}", e.getMessage());
        }
    }

    private void markConnected() {
        isConnected = true;
    }

    private void markDisconnected() {
        isConnected = false;
        reconnecting.set(false);
        reconnectAttempt.set(0);
    }

    private WebSocketSession findOpenSession() throws IllegalStateException {
        return webSocketHandler.getSessions().values().stream()
                .filter(WebSocketSession::isOpen)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No open WebSocket session found"));
    }

    private void scheduleReconnectAttempt(String lastServerUrl, Runnable callback) {
        Duration delay = nextBackoffDelay(reconnectAttempt.getAndIncrement());

        reconnectScheduler.schedule(
                () -> {
                    try {
                        log.info(
                                "Reconnect attempt #{} (delay {}ms)",
                                reconnectAttempt.get(),
                                delay.toMillis());
                        connect(lastServerUrl);
                    } catch (Exception e) {
                        log.error("Reconnect attempt failed", e);
                        scheduleReconnectAttempt(lastServerUrl, callback); // keep trying
                    }
                    connectionFuture
                            .orTimeout(reconnectAttempt.get(), TimeUnit.SECONDS)
                            .thenRun(
                                    () -> {
                                        reconnectAttempt.set(0);
                                        markConnected();
                                        callback.run();
                                    })
                            .exceptionally(
                                    (_) -> {
                                        scheduleReconnectAttempt(lastServerUrl, callback);
                                        return null;
                                    });
                },
                delay.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    private Duration nextBackoffDelay(int attempt) {
        long baseMs = RECONNECT_BASE_DELAY.toMillis();
        long maxMs = RECONNECT_MAX_DELAY.toMillis();

        long expMs = (attempt >= 30) ? maxMs : Math.min(maxMs, baseMs * (1L << attempt));

        return Duration.ofMillis(expMs);
    }

    public void reconnectToServerWithToken(String serverUrl, String reconnectPlay) {
        Runnable callback =
                () -> {
                    sendMessage(reconnectPlay);
                };
        reconnect(serverUrl, callback);
    }

    public CompletableFuture<Void> getConnectionCallback() {
        return connectionFuture;
    }
}
