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

    public SolaceListener(SettlementService settlementService, ObjectMapper objectMapper,
                          JCSMPSession solaceSession) throws JCSMPException {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;

        Queue queue = JCSMPFactory.onlyInstance().createQueue("trading.fx.trade.submitted");
        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_AUTO);

        FlowReceiver flowReceiver = solaceSession.createFlow(this, flowProps);
        flowReceiver.start();

        log.info("✅ Connected and listening to queue: trading.fx.trade.submitted");
    }

    @Override
    public void onReceive(BytesXMLMessage msg) {
        try {
            if (msg instanceof TextMessage textMsg) {
                String payload = textMsg.getText();
                log.info("📨 Received from queue: {}", payload);
                msg.ackMessage();

                TradeSubmittedEvent event = objectMapper.readValue(payload, TradeSubmittedEvent.class);
                onTradeSubmitted(event);
            }
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
        }
    }

    @Override
    public void onException(JCSMPException e) {
        log.error("❌ Solace error: {}", e.getMessage());
    }

    public void onTradeSubmitted(TradeSubmittedEvent event) {
        try {
            log.info("⚙️  Processing: {}", event.tradeId());
            settlementService.processTradeSubmitted(event);
            log.info("✅ Settlement: {}", event.tradeId());
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
        }
    }
}
