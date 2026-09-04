package com.hosseingorji.expensetracker;

import com.hosseingorji.expensetracker.model.Expense;
import com.hosseingorji.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void advice_withValidBody_returns200AndPlan() throws Exception {
        repository.save(new Expense("Groceries", 300.0, "Food", LocalDate.now().withDayOfMonth(1)));

        mockMvc.perform(post("/api/budget/advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyBudget\":1000,\"cutTarget\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSpend").value(300.0))
                .andExpect(jsonPath("$.breakdown[0].category").value("Food"))
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    void advice_withMissingBudget_returns400() throws Exception {
        mockMvc.perform(post("/api/budget/advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutTarget\":100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void advice_withNegativeCutTarget_returns400() throws Exception {
        mockMvc.perform(post("/api/budget/advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyBudget\":1000,\"cutTarget\":-50}"))
                .andExpect(status().isBadRequest());
    }
}
