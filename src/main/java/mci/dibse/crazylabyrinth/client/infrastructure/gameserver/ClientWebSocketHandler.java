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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("Received message from session {}: {}", session.getId(), message.getPayload());
        if(!hasActiveSessions() || !canReceiveMessages) {
            log.warn("No active sessions. Ignoring message");
            return;
        }
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
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        canReceiveMessages = true;
        sessions.put(session.getId(), session);
        if (connectionCallback != null) {
            connectionCallback.complete(null);
        }
        log.debug("WebSocket connection established with session: {}", session.getId());
    }

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

    @EventListener
    public void handleDisconnectEvent(CommandType event) {
        if(CommandType.DISCONNECT.equals(event)) {
            canReceiveMessages = false;
            log.debug("Handle disconnect event");
            closeSession();
        };
    }

    public boolean hasActiveSessions() {
        // Remove any closed sessions
        sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());
        return !sessions.isEmpty();
    }

    public void setBlockForReceivingMessages(boolean canReceive) {
        this.canReceiveMessages = canReceive;
    }
}
