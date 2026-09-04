package com.hosseingorji.expensetracker.service;

import com.hosseingorji.expensetracker.model.Expense;
import com.hosseingorji.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public List<Expense> getAll() {
        return repository.findAll();
    }

    public Optional<Expense> getById(Long id) {
        return repository.findById(id);
    }

    public List<Expense> getByCategory(String category) {
        return repository.findByCategory(category);
    }

    public Expense create(Expense expense) {
        return repository.save(expense);
    }

    public Optional<Expense> update(Long id, Expense updated) {
        return repository.findById(id).map(existing -> {
            existing.setDescription(updated.getDescription());
            existing.setAmount(updated.getAmount());
            existing.setCategory(updated.getCategory());
            existing.setDate(updated.getDate());
            return repository.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
