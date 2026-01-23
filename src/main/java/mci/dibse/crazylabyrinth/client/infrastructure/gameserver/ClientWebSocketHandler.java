package mci.dibse.crazylabyrinth.client.infrastructure.gameserver;

import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.CONNECTION_LOST;
import static mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents.TURN_TIMEOUT;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import labyrinth.contracts.models.CommandType;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.domain.context.ApplicationEvents;
import mci.dibse.crazylabyrinth.client.service.ServerEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class ClientWebSocketHandler extends TextWebSocketHandler {

    private boolean canReceiveMessages = true;

    private final ApplicationEventPublisher eventListener;

    @Getter private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Setter @Getter private volatile CompletableFuture<Void> connectionCallback;

    @Autowired private ServerEventService serverEventService;

    @Autowired
    @Qualifier("webSocketExecutor")
    private Executor webSocketExecutor;

    @Autowired
    public ClientWebSocketHandler(ApplicationEventPublisher eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * Handle incoming text messages from the WebSocket.
     *
     * @param session WebSocket session
     * @param message received message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("Received message from session {}: {}", session.getId(), message.getPayload());
        if(!hasActiveSessions() || !canReceiveMessages) {
            log.warn("No active sessions. Ignoring message");
            return;
        }
        /*
        webSocketExecutor.execute(
                () -> {
                    try {

                        serverEventService.handleEvent(message.getPayload());
                    } catch (Exception e) {
                        log.error(
                                "Something went terribly wrong. Should have never happened. Very"
                                        + " sorry",
                                e);
                    }
                });

         */

        try {

            serverEventService.handleEvent(message.getPayload());
        } catch (Exception e) {
            log.error(
                    "Something went terribly wrong. Should have never happened. Very"
                            + " sorry",
                    e);
        }
    }

    /**
     * Register a session and complete the connection callback.
     *
     * @param session new WebSocket session
     * @throws Exception when handler errors occur
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        session.setTextMessageSizeLimit(1024 * 1024);

        canReceiveMessages = true;
        sessions.put(session.getId(), session);
        if (connectionCallback != null) {
            connectionCallback.complete(null);
        }
        log.debug("WebSocket connection established with session: {}", session.getId());
    }

    /**
     * Handle session closure and publish disconnect events when needed.
     *
     * @param session closed session
     * @param status close status
     * @throws Exception when handler errors occur
     */
    @Override
    public void afterConnectionClosed(
            @NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessions.remove(session.getId());
        log.info(
                "WebSocket connection closed for session: {} with status: {}",
                session.getId(),
                status);

        // WebSocket connection closed for session: 1b8607c2-7211-2684-7499-3ef9db732f7e with
        // status: CloseStatus[code=1008, reason=Disconnected due to turn timeout.]
        if (status.getCode() == 1008) {
            eventListener.publishEvent(TURN_TIMEOUT);
        } else if (status.getCode() != 1000) {
            eventListener.publishEvent(CONNECTION_LOST);
            connectionCallback = null;
        }
        canReceiveMessages = false;
    }

    /**
     * Handle transport errors and publish connection lost events.
     *
     * @param session WebSocket session
     * @param exception transport exception
     * @throws Exception when handler errors occur
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        log.error("Transport error for session {}: {}", session.getId(), exception.getMessage());
        if (connectionCallback != null) {
            connectionCallback.completeExceptionally(exception);
        }
        // notify the client?
        sessions.remove(session.getId());
        eventListener.publishEvent(CONNECTION_LOST);
        canReceiveMessages = false;
    }

    /**
     * Close all open sessions and clear the session map.
     */
    public void closeSession() {
        sessions.values()
                .forEach(
                        session -> {
                            if (session.isOpen()) {
                                try {
                                    session.close(CloseStatus.NORMAL);
                                } catch (Exception e) {
                                    log.error(
                                            "Error closing session {}: {}",
                                            session.getId(),
                                            e.getMessage());
                                }
                            }
                        });
        sessions.clear();
    }

    /**
     * Handle a disconnect command by closing sessions.
     *
     * @param event disconnect command
     */
    @EventListener
    public void handleDisconnectEvent(CommandType event) {
        if(CommandType.DISCONNECT.equals(event)) {
            canReceiveMessages = false;
            log.debug("Handle disconnect event");
            closeSession();
        };
    }

    /**
     * Check if any active sessions remain.
     *
     * @return true when at least one session is open
     */
    public boolean hasActiveSessions() {
        // Remove any closed sessions
        sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());
        return !sessions.isEmpty();
    }

    /**
     * Enable or disable receiving messages.
     *
     * @param canReceive true to receive messages
     */
    public void setBlockForReceivingMessages(boolean canReceive) {
        this.canReceiveMessages = canReceive;
    }
}
