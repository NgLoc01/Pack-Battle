package io.github.ngloc01.pack_battle_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");              //topic --> when server sends to clients (stompClient.subscribe(...))
        config.setApplicationDestinationPrefixes("/app"); //app   --> when clients send to server (@MessageMapping)

        //config.enableSimpleBroker("/topic", "/queue");
        //config.setApplicationDestinationPrefixes("/app");
        //config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/pack-websocket").withSockJS();
    }
}
