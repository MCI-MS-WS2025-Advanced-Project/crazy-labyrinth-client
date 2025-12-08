package mci.dibse.crazylabyrinth.client.infrastructure.util;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author Julian Janetschek
 */
@Component
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /** Handler for game WebSocket connections. */
    // private final GameWebSocketHandler gameWebSocketHandler;
    // private final String websocketEndpoint;
    // private final String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // registry.addHandler()
    }
}
