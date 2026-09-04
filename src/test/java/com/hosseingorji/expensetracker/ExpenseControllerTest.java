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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    /** Build a request JSON body by hand so the test doesn't depend on a JSON library. */
    private String json(Expense e) {
        return String.format(
                "{%s,%s,%s,%s}",
                field("description", e.getDescription()),
                numField("amount", e.getAmount()),
                field("category", e.getCategory()),
                field("date", e.getDate() == null ? null : e.getDate().toString()));
    }

    private String field(String name, String value) {
        return value == null ? "\"" + name + "\":null" : "\"" + name + "\":\"" + value + "\"";
    }

    private String numField(String name, Double value) {
        return value == null ? "\"" + name + "\":null" : "\"" + name + "\":" + value;
    }

    private Long createExpense(String desc, Double amount, String category, LocalDate date) {
        return repository.save(new Expense(desc, amount, category, date)).getId();
    }

    // --- create ---

    @Test
    void createExpense_withValidData_returns201() throws Exception {
        Expense expense = new Expense("Groceries", 45.50, "Food", LocalDate.of(2026, 8, 30));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description").value("Groceries"))
                .andExpect(jsonPath("$.amount").value(45.50));
    }

    @Test
    void createExpense_withNegativeAmount_returns400() throws Exception {
        Expense expense = new Expense("Bad expense", -10.0, "Food", LocalDate.of(2026, 8, 30));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(expense)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExpense_withBlankDescription_returns400() throws Exception {
        Expense expense = new Expense("", 20.0, "Food", LocalDate.of(2026, 8, 30));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(expense)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExpense_withNullDate_returns400() throws Exception {
        Expense expense = new Expense("No date", 5.0, "Food", null);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(expense)))
                .andExpect(status().isBadRequest());
    }

    // --- read ---

    @Test
    void getAllExpenses_returns200() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk());
    }

    @Test
    void getExpenseById_whenFound_returns200() throws Exception {
        Long id = createExpense("Coffee", 4.0, "Food", LocalDate.of(2026, 8, 30));

        mockMvc.perform(get("/api/expenses/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Coffee"));
    }

    @Test
    void getExpenseById_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/expenses/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExpenses_filteredByCategory_returnsOnlyMatching() throws Exception {
        createExpense("Groceries", 40.0, "Food", LocalDate.of(2026, 8, 30));
        createExpense("Dinner", 30.0, "Food", LocalDate.of(2026, 8, 31));
        createExpense("Gas", 60.0, "Transport", LocalDate.of(2026, 8, 31));

        mockMvc.perform(get("/api/expenses").param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].category", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Food"))));
    }

    // --- update ---

    @Test
    void updateExpense_whenFound_returns200AndUpdatedFields() throws Exception {
        Long id = createExpense("Old", 10.0, "Food", LocalDate.of(2026, 8, 30));
        Expense updated = new Expense("New", 25.0, "Transport", LocalDate.of(2026, 9, 1));

        mockMvc.perform(put("/api/expenses/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("New"))
                .andExpect(jsonPath("$.amount").value(25.0))
                .andExpect(jsonPath("$.category").value("Transport"));
    }

    @Test
    void updateExpense_whenNotFound_returns404() throws Exception {
        Expense updated = new Expense("Nope", 5.0, "Food", LocalDate.of(2026, 9, 1));

        mockMvc.perform(put("/api/expenses/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updated)))
                .andExpect(status().isNotFound());
    }

    // --- delete ---

    @Test
    void deleteExpense_whenFound_returns204() throws Exception {
        Long id = createExpense("Temp", 1.0, "Misc", LocalDate.of(2026, 9, 1));

        mockMvc.perform(delete("/api/expenses/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/expenses/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExpense_whenNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/expenses/99999"))
                .andExpect(status().isNotFound());
    }

    // --- categorize ---
    // The endpoint always returns one of the allowed categories. The AI call
    // itself (and the fallback behaviour) is covered in CategorizationServiceTest
    // with a mocked HTTP server; here we only assert the contract.

    @Test
    void categorize_withValidDescription_returns200AndAllowedCategory() throws Exception {
        mockMvc.perform(post("/api/expenses/categorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Uber Eats order, $23.50\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedCategory", org.hamcrest.Matchers.in(
                        com.hosseingorji.expensetracker.service.CategorizationService.ALLOWED_CATEGORIES)))
                .andExpect(jsonPath("$.reasoning").isNotEmpty());
    }

    @Test
    void categorize_withBlankDescription_returns400() throws Exception {
        mockMvc.perform(post("/api/expenses/categorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
