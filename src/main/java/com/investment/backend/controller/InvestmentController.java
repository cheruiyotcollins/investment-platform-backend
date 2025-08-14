package com.investment.backend.controller;

import com.investment.backend.dto.InvestRequest;
import com.investment.backend.dto.InvestmentResponse;
import com.investment.backend.service.InvestmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService service;

    public InvestmentController(InvestmentService service) {
        this.service = service;
    }

    /**
     * Frontend calls this (invest).
     * Example body: { "planId": 1, "amount": 100, "asset": "USDT" }
     */
    @PostMapping
    public ResponseEntity<?> invest(@AuthenticationPrincipal(expression = "username") String username,
                                    @RequestBody InvestRequest request) {
        try {
            // If you don't use AuthenticationPrincipal, replace username retrieval with Principal name:
            // String username = principal.getName();
            var resp = service.invest(username, request);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage()); // conflict / insufficient funds
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to create investment: " + e.getMessage());
        }
    }

    /**
     * Return investment history for current user
     */
    @GetMapping("/history")
    public ResponseEntity<?> history(@AuthenticationPrincipal(expression = "username") String username) {
        try {
            List<InvestmentResponse> list = service.getHistory(username);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch history: " + e.getMessage());
        }
    }
}
