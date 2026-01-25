package mci.dibse.crazylabyrinth.client.infrastructure.util;

import mci.dibse.crazylabyrinth.client.infrastructure.gameserver.ClientWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author Julian Janetschek
 */

/*
//@Component
//@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {


    private final String websocketEndpoint;
    private final String allowedOrigins;
    private final ClientWebSocketHandler clientWebSocketHandler;

    @Autowired
    public WebSocketConfig(
            @Value("${game.server.websocket.endpoint}") String websocketEndpoint,
            @Value("${game.server.websocket.allowedOrigins}") String allowedOrigins,
            ClientWebSocketHandler clientWebSocketHandler) {
        this.clientWebSocketHandler = clientWebSocketHandler;
        this.websocketEndpoint = websocketEndpoint;
        this.allowedOrigins = "*";
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
         registry.addHandler(clientWebSocketHandler, websocketEndpoint)
                 .setAllowedOrigins(allowedOrigins);
    }

}

 */
