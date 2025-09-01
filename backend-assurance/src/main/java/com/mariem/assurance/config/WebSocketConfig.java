package com.mariem.assurance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Version sans conflit de nom et sans cycle :
 * - On DÉFINIT notre propre scheduler "wsTaskScheduler" (≠ messageBrokerTaskScheduler).
 * - On l'injecte dans le simple broker via setTaskScheduler(...).
 * - Aucun impact sur le reste de l’app.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_INTERVAL = 25_000L; // 25s
    private static final long DISCONNECT_DELAY   = 5_000L;  // 5s

    @Bean(name = "wsTaskScheduler")
    public TaskScheduler wsTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setPoolSize(1);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL})
                .setTaskScheduler(wsTaskScheduler()); // on utilise NOTRE bean, pas celui de Spring

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(HEARTBEAT_INTERVAL)
                .setDisconnectDelay(DISCONNECT_DELAY);

        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*");
    }
}
