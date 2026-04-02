package com.gestiontickets.tickets.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //  Prefix pour les topics que le client écoute
        config.enableSimpleBroker("/topic");
        //  Prefix pour les messages envoyés par le client
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //  Endpoint WebSocket avec SockJS fallback
        registry.addEndpoint("/ws-dashboard")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}