package com.atomicfx.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SolaceListener implements XMLMessageListener {
    private static final Logger log = LoggerFactory.getLogger(SolaceListener.class);
    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;
    private final JCSMPSession solaceSession;

    public SolaceListener(SettlementService settlementService, ObjectMapper objectMapper,
                          JCSMPSession solaceSession) {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
        this.solaceSession = solaceSession;
        subscribe();
    }

    private void subscribe() {
        try {
            Queue queue = JCSMPFactory.onlyInstance().createQueue("trading.fx.trade.submitted");
            solaceSession.addSubscription(queue);
            XMLMessageConsumer consumer = solaceSession.getMessageConsumer(this);
            log.info("Subscribed to trading.fx.trade.submitted queue");
        } catch (JCSMPException e) {
            log.error("Subscribe failed", e);
        }
    }

    @Override
    public void onReceive(BytesXMLMessage msg) {
        try {
            if (msg instanceof TextMessage textMsg) {
                String payload = textMsg.getText();
                log.info("Received: {}", payload);
                processMessage(payload);
            }
        } catch (Exception e) {
            log.error("Receive error", e);
        }
    }

    @Override
    public void onException(JCSMPException e) {
        log.error("Solace error", e);
    }

    private void processMessage(String payload) {
        try {
            TradeSubmittedEvent event = objectMapper.readValue(payload, TradeSubmittedEvent.class);
            onTradeSubmitted(event);
        } catch (Exception e) {
            log.error("Parse error", e);
        }
    }

    public void onTradeSubmitted(TradeSubmittedEvent event) {
        try {
            log.info("Processing: {}", event.tradeId());
            settlementService.processTradeSubmitted(event);
            log.info("Settlement: {}", event.tradeId());
        } catch (Exception e) {
            log.error("Error", e);
        }
    }
}
