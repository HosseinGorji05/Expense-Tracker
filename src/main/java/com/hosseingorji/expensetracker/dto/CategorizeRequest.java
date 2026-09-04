package com.hosseingorji.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategorizeRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description is too long")
    private String description;

    public CategorizeRequest() {}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
