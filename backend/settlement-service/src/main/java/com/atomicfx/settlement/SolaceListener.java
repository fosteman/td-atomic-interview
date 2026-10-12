package com.atomicfx.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SolaceListener implements XMLMessageListener {
    private static final Logger log = LoggerFactory.getLogger(SolaceListener.class);
    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    public SolaceListener(SettlementService settlementService, ObjectMapper objectMapper,
                          JCSMPSession solaceSession) {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
        try {
            solaceSession.getMessageConsumer(this);
            log.info("Listening on trading.fx.trade.submitted");
        } catch (JCSMPException e) {
            log.error("Init failed", e);
        }
    }

    @Override
    public void onReceive(BytesXMLMessage msg) {
        try {
            if (msg instanceof TextMessage textMsg) {
                String payload = textMsg.getText();
                log.info("Received: {}", payload);
                msg.ackMessage();
                TradeSubmittedEvent event = objectMapper.readValue(payload, TradeSubmittedEvent.class);
                onTradeSubmitted(event);
            }
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    @Override
    public void onException(JCSMPException e) {
        log.error("Solace error", e);
    }

    public void onTradeSubmitted(TradeSubmittedEvent event) {
        try {
            log.info("Processing: {}", event.tradeId());
            settlementService.processTradeSubmitted(event);
            log.info("Settlement: {}", event.tradeId());
        } catch (Exception e) {
            log.error("Processing error", e);
        }
    }
}
