package com.atomicfx.settlement;

import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Solace JCSMP Configuration for Settlement Service (Consumer)
 * Uses native Solace SMF protocol via JCSMP API.
 */
@Configuration
public class SolaceJmsConfig {
    private static final Logger log = LoggerFactory.getLogger(SolaceJmsConfig.class);

    @Bean
    public JCSMPSession solaceSession() throws JCSMPException {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, "localhost:55554");
        properties.setProperty(JCSMPProperties.USERNAME, "admin");
        properties.setProperty(JCSMPProperties.PASSWORD, "admin");
        properties.setProperty(JCSMPProperties.VPN_NAME, "default");

        JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        log.info("Connected to Solace via JCSMP");
        return session;
    }
}
