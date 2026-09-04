package com.hosseingorji.expensetracker.dto;

public class CategorizeResponse {

    private String suggestedCategory;
    private String reasoning;

    public CategorizeResponse() {}

    public CategorizeResponse(String suggestedCategory, String reasoning) {
        this.suggestedCategory = suggestedCategory;
        this.reasoning = reasoning;
    }

    public String getSuggestedCategory() { return suggestedCategory; }
    public void setSuggestedCategory(String suggestedCategory) { this.suggestedCategory = suggestedCategory; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
}
