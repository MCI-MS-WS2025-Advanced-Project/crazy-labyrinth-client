package mci.dibse.crazylabyrinth.client.infrastructure.gameserver;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.service.ServerEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class ClientWebSocketHandler extends TextWebSocketHandler {

    @Getter
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Setter @Getter
    private volatile CompletableFuture<Void> connectionCallback;

    @Autowired
    private ServerEventService serverEventService;
    @Autowired
    @Qualifier("webSocketExecutor")
    private Executor webSocketExecutor;



    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("Received message from session {}: {}", session.getId(), message.getPayload());
        //serverEventService.handleEvent(message.getPayload());
        webSocketExecutor.execute(() -> {
            try {
                serverEventService.handleEvent(message.getPayload());
            } catch (Exception e) {
                log.error("Something went terribly wrong. Should have never happened. Very sorry", e);
            }
        });


    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //TODO
        System.out.println("afterConnectionEstablished");
        super.afterConnectionEstablished(session);
        sessions.put(session.getId(), session);
        if (connectionCallback != null) {
            connectionCallback.complete(null);
        }
        log.debug("WebSocket connection established with session: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessions.remove(session.getId());
        log.info("WebSocket connection closed for session: {} with status: {}",
                session.getId(), status);

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error for session {}: {}", session.getId(), exception.getMessage());
        if (connectionCallback != null) {
            connectionCallback.completeExceptionally(exception);
        }
        // notify the client?
        sessions.remove(session.getId());
    }


    public void closeSession() {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.close(CloseStatus.NORMAL);
                } catch (Exception e) {
                    log.error("Error closing session {}: {}", session.getId(), e.getMessage());
                }
            }
        });
        sessions.clear();
    }
    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public boolean hasActiveSessions() {
        // Remove any closed sessions
        sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());
        return !sessions.isEmpty();
    }


}
    

