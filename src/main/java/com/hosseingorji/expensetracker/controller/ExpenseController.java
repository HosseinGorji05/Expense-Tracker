package com.hosseingorji.expensetracker.controller;

import com.hosseingorji.expensetracker.model.Expense;
import com.hosseingorji.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<Expense> getAll(@RequestParam(required = false) String category) {
        if (category != null) {
            return service.getByCategory(category);
        }
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Expense> create(@Valid @RequestBody Expense expense) {
        Expense saved = service.create(expense);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> update(@PathVariable Long id, @Valid @RequestBody Expense expense) {
        return service.update(id, expense)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
