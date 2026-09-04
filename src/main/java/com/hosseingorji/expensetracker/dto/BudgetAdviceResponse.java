package com.hosseingorji.expensetracker.dto;

import java.util.List;

public class BudgetAdviceResponse {

    private double monthlyBudget;
    private double cutTarget;
    private double currentSpend;
    private double overBudgetBy;          // currentSpend - monthlyBudget, 0 if under budget
    private double totalSuggestedCuts;
    private double projectedSpendAfterCuts;
    private boolean aiGenerated;          // false => deterministic fallback was used
    private List<CategorySpend> breakdown;
    private List<CutSuggestion> suggestions;
    private String summary;

    public BudgetAdviceResponse() {}

    public static class CategorySpend {
        private String category;
        private double amount;

        public CategorySpend() {}
        public CategorySpend(String category, double amount) { this.category = category; this.amount = amount; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }

    public static class CutSuggestion {
        private String category;
        private double currentSpend;
        private double suggestedCut;
        private String reason;

        public CutSuggestion() {}
        public CutSuggestion(String category, double currentSpend, double suggestedCut, String reason) {
            this.category = category;
            this.currentSpend = currentSpend;
            this.suggestedCut = suggestedCut;
            this.reason = reason;
        }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public double getCurrentSpend() { return currentSpend; }
        public void setCurrentSpend(double currentSpend) { this.currentSpend = currentSpend; }
        public double getSuggestedCut() { return suggestedCut; }
        public void setSuggestedCut(double suggestedCut) { this.suggestedCut = suggestedCut; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(double monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public double getCutTarget() { return cutTarget; }
    public void setCutTarget(double cutTarget) { this.cutTarget = cutTarget; }
    public double getCurrentSpend() { return currentSpend; }
    public void setCurrentSpend(double currentSpend) { this.currentSpend = currentSpend; }
    public double getOverBudgetBy() { return overBudgetBy; }
    public void setOverBudgetBy(double overBudgetBy) { this.overBudgetBy = overBudgetBy; }
    public double getTotalSuggestedCuts() { return totalSuggestedCuts; }
    public void setTotalSuggestedCuts(double totalSuggestedCuts) { this.totalSuggestedCuts = totalSuggestedCuts; }
    public double getProjectedSpendAfterCuts() { return projectedSpendAfterCuts; }
    public void setProjectedSpendAfterCuts(double projectedSpendAfterCuts) { this.projectedSpendAfterCuts = projectedSpendAfterCuts; }
    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
    public List<CategorySpend> getBreakdown() { return breakdown; }
    public void setBreakdown(List<CategorySpend> breakdown) { this.breakdown = breakdown; }
    public List<CutSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<CutSuggestion> suggestions) { this.suggestions = suggestions; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
