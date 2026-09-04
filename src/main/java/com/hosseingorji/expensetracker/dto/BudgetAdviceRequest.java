package com.hosseingorji.expensetracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class BudgetAdviceRequest {

    @NotNull(message = "Monthly budget is required")
    @Positive(message = "Monthly budget must be greater than zero")
    private Double monthlyBudget;

    @NotNull(message = "Cut target is required")
    @PositiveOrZero(message = "Cut target cannot be negative")
    private Double cutTarget;

    public BudgetAdviceRequest() {}

    public Double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(Double monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public Double getCutTarget() { return cutTarget; }
    public void setCutTarget(Double cutTarget) { this.cutTarget = cutTarget; }
}
