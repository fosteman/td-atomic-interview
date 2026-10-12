package com.atomicfx.config;

import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolaceConfig {
    private static final Logger log = LoggerFactory.getLogger(SolaceConfig.class);

    @Bean
    public JCSMPSession solaceSession() throws JCSMPException {
        JCSMPProperties props = new JCSMPProperties();
        props.setProperty(JCSMPProperties.HOST, "localhost:55554");
        props.setProperty(JCSMPProperties.USERNAME, "admin");
        props.setProperty(JCSMPProperties.PASSWORD, "admin");
        props.setProperty(JCSMPProperties.VPN_NAME, "default");

        JCSMPSession session = JCSMPFactory.onlyInstance().createSession(props);
        session.connect();
        log.info("Connected to Solace");
        return session;
    }
}
