package com.atomicfx.position;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@CrossOrigin(origins = "http://localhost:5173")
public class PositionController {
    private final PositionRepository positionRepository;

    public PositionController(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @GetMapping
    public ResponseEntity<List<PositionResponse>> getPositions() {
        String accountId = "account-123"; // Mock
        List<PositionResponse> positions = positionRepository
            .findByAccountId(accountId) // pattern of python-list()
            .stream() // stream processing
            .map(PositionResponse::from) // transform
            .toList(); // collects
        return ResponseEntity.ok(positions);
    }
}
