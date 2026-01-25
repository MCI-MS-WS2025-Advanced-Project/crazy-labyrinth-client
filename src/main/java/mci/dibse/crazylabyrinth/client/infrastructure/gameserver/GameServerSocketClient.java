package mci.dibse.crazylabyrinth.client.infrastructure.gameserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GameServerSocketClient {

    //TODO keep session in handler?
    //private WebSocketSession session;

    private static final String GAME_PATH = "/game";
    private static final int DELAY_AFTER_FIRST_CONNECT = 1;

    private final ClientWebSocketHandler webSocketHandler;

    private WebSocketClient webSocketClient;


    /*
    *   What WebSocketConnectionManager does dos do
            1. **Connection Lifecycle**: Handles starting/stopping the connection as part of Spring's lifecycle
            2. **Integration Layer**: Bridges Spring's lifecycle management with WebSocket clients
            3. **Resource Management**: Manages the underlying WebSocket client resources
       Does NOT Do:
            1. **No Auto-Reconnection**: It doesn't automatically reconnect when connections are lost
            2. **No Error Recovery**: It doesn't handle transport errors or connection failures beyond logging them
            3. **No Connection Monitoring**: It doesn't actively monitor connection health
     */
    private WebSocketConnectionManager connectionManager;


    private volatile boolean isConnected = false;
    private volatile boolean connectionLost = false;

    private CompletableFuture<Void> connectionFuture;

    @Autowired
    public GameServerSocketClient(ClientWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }


    public void sendMessage(String message) {
        if (!isConnected || !webSocketHandler.hasActiveSessions()) {
            log.warn("Cannot send message: WebSocket is not connected");
            // TODO: impl reconnect and resend logic
            // reconnect();
            sendMessageWithDelay(message, DELAY_AFTER_FIRST_CONNECT);
        }

        try {
            WebSocketSession session = webSocketHandler.getSessions()
                    .values()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active WebSocket session found"));

            session.sendMessage(new TextMessage(message));
            log.debug("Message sent successfully: {}", message);


            //TODO impl erro handling
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            log.error("No active WebSocket session found");
        }

    }


    public void connect(String serverUrl) {
        System.out.println("Connecting to WebSocket server at: " + serverUrl);
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
                    .thenRun(() -> {
                        isConnected = true;
                        connectionLost = false;
                        log.info("Successfully connected to WebSocket server");
                    })
                    .orTimeout(5, TimeUnit.SECONDS) // Built-in timeout
                    .exceptionally(throwable -> {
                        log.error("Failed to connect to WebSocket server: {}", throwable.getMessage());
                        isConnected = false;
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("Failed to connect to WebSocket server: " + e.getMessage());
            connectionFuture.completeExceptionally(e);
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

            isConnected = false;
            connectionLost = false;

        } catch (Exception e) {
            log.error("Error during disconnect: {}", e.getMessage());
        }
    }


    //TODO:
    public void reconnect() {
    }


    private void sendMessageWithDelay(String message, long delaySeconds) {
        CompletableFuture
                .runAsync(() -> sendMessage(message),
                        CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS));

    }

}
// ###########################################################################
// ###########################################################################

/*
    public void connect(String url) {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                client,
                new ClientWebSocketHandler(),
                url
        );
        manager.start();


        // Or connect directly
        try {
            session = client.doHandshake(
                    new MyWebSocketHandler(),
                    null,
                    URI.create("ws://localhost:8080/game-websocket")
            ).get();
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> connect(String serverUrl) {
        WebSocketHandler handler = new ClientWebSocketHandler();

        return webSocketClient.doHandshake(handler, null, URI.create(serverUrl))
                .thenAccept(webSocketSession -> {
                    this.session = webSocketSession;
                    System.out.println("Connected to: " + serverUrl);
                })
                .exceptionally(throwable -> {
                    System.err.println("Connection failed: " + throwable.getMessage());
                    return null;
                });
    }


    public void sendMessage(String message) throws Exception {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    public void connectToGameServer() {
        try {
            WebSocketHandler handler = new GameClientHandler();

            CompletableFuture<WebSocketSession> future =
                    webSocketClient.doHandshake(handler, null, URI.create(GAME_SERVER_URL));

            this.session = future.get(5, TimeUnit.SECONDS); // Wait up to 5 seconds
            System.out.println("Successfully connected to game server");

        } catch (Exception e) {
            System.err.println("Failed to connect to game server: " + e.getMessage());
        }
    }

    public void joinGame(String playerId) {
        sendMessage("JOIN:" + playerId);
    }

    public void sendGameMove(String move) {
        sendMessage("MOVE:" + move);
    }

 */



