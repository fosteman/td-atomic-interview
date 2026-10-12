package com.atomicfx.settlement;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlements")
@CrossOrigin(origins = "http://localhost:5173")
public class SettlementController {
    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * GET /api/settlements/{tradeId}
     * Get settlement details for a trade
     */
    @GetMapping("/{tradeId}")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable String tradeId) {
        try {
            Settlement settlement = settlementService.getSettlement(tradeId);
            return ResponseEntity.ok(SettlementResponse.from(settlement));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/settlements/{settlementId}/instruct
     * Mark settlement as instructed (sent to bank)
     */
    @PostMapping("/{settlementId}/instruct")
    public ResponseEntity<SettlementResponse> instruct(@PathVariable String settlementId) {
        try {
            settlementService.markInstructed(settlementId);
            Settlement settlement = settlementService.getSettlement(settlementId);
            return ResponseEntity.ok(SettlementResponse.from(settlement));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/settlements/{settlementId}/settle
     * Mark settlement as complete
     */
    @PostMapping("/{settlementId}/settle")
    public ResponseEntity<SettlementResponse> settle(@PathVariable String settlementId) {
        try {
            settlementService.markSettled(settlementId);
            Settlement settlement = settlementService.getSettlement(settlementId);
            return ResponseEntity.ok(SettlementResponse.from(settlement));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
