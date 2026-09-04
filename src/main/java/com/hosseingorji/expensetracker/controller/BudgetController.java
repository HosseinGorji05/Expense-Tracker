package com.hosseingorji.expensetracker.controller;

import com.hosseingorji.expensetracker.dto.BudgetAdviceRequest;
import com.hosseingorji.expensetracker.dto.BudgetAdviceResponse;
import com.hosseingorji.expensetracker.service.BudgetAdvisorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetAdvisorService advisor;

    public BudgetController(BudgetAdvisorService advisor) {
        this.advisor = advisor;
    }

    @PostMapping("/advice")
    public ResponseEntity<BudgetAdviceResponse> advice(@Valid @RequestBody BudgetAdviceRequest request) {
        return ResponseEntity.ok(
                advisor.advise(request.getMonthlyBudget(), request.getCutTarget()));
    }
}
