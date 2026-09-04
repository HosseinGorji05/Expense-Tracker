package com.hosseingorji.expensetracker;

import com.hosseingorji.expensetracker.dto.BudgetAdviceResponse;
import com.hosseingorji.expensetracker.model.Expense;
import com.hosseingorji.expensetracker.repository.ExpenseRepository;
import com.hosseingorji.expensetracker.service.BudgetAdvisorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
        "groq.api.key=test-key",
        "groq.api.url=https://api.groq.com/openai/v1/chat/completions"
})
class BudgetAdvisorServiceTest {

    @Autowired
    private BudgetAdvisorService advisor;

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    private void seedThisMonth(String category, double amount) {
        LocalDate today = LocalDate.now();
        repository.save(new Expense(category + " spend", amount, category, today.withDayOfMonth(1)));
    }

    private void stubGroq(String content) {
        String json = "{\"choices\":[{\"message\":{\"content\":%s}}]}".formatted(
                jsonString(content));
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    @Test
    void advise_usesLlmSuggestionsWhenAvailable() {
        seedThisMonth("Food", 600);
        seedThisMonth("Entertainment", 200);
        seedThisMonth("Housing", 1000);

        stubGroq("{\"suggestions\":[" +
                "{\"category\":\"Entertainment\",\"suggestedCut\":120,\"reason\":\"cut streaming\"}," +
                "{\"category\":\"Food\",\"suggestedCut\":80,\"reason\":\"eat out less\"}]}");

        BudgetAdviceResponse res = advisor.advise(1500, 200);

        assertThat(res.isAiGenerated()).isTrue();
        assertThat(res.getCurrentSpend()).isEqualTo(1800.0);
        assertThat(res.getOverBudgetBy()).isEqualTo(300.0);
        assertThat(res.getSuggestions()).extracting("category")
                .containsExactly("Entertainment", "Food");
        assertThat(res.getTotalSuggestedCuts()).isEqualTo(200.0);
        assertThat(res.getProjectedSpendAfterCuts()).isEqualTo(1600.0);
    }

    @Test
    void advise_clampsLlmCutToCategorySpend() {
        seedThisMonth("Entertainment", 50);

        stubGroq("{\"suggestions\":[{\"category\":\"Entertainment\",\"suggestedCut\":9999,\"reason\":\"x\"}]}");

        BudgetAdviceResponse res = advisor.advise(1000, 500);

        assertThat(res.getSuggestions()).hasSize(1);
        assertThat(res.getSuggestions().get(0).getSuggestedCut()).isEqualTo(50.0);
    }

    @Test
    void advise_ignoresUnknownCategoryFromLlm() {
        seedThisMonth("Food", 300);

        stubGroq("{\"suggestions\":[{\"category\":\"Crypto\",\"suggestedCut\":100,\"reason\":\"x\"}]}");

        BudgetAdviceResponse res = advisor.advise(1000, 100);

        // Nothing valid from the LLM -> deterministic fallback kicks in.
        assertThat(res.isAiGenerated()).isFalse();
        assertThat(res.getSuggestions()).allSatisfy(s ->
                assertThat(s.getCategory()).isEqualTo("Food"));
    }

    @Test
    void advise_fallsBackToHeuristicWhenApiFails() {
        seedThisMonth("Shopping", 400);
        seedThisMonth("Housing", 800);

        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withServerError());

        BudgetAdviceResponse res = advisor.advise(1000, 100);

        assertThat(res.isAiGenerated()).isFalse();
        // Discretionary (Shopping) is trimmed before the essential (Housing).
        assertThat(res.getSuggestions().get(0).getCategory()).isEqualTo("Shopping");
        assertThat(res.getTotalSuggestedCuts()).isEqualTo(100.0);
        // No single cut exceeds 40% of that category's spend.
        assertThat(res.getSuggestions()).allSatisfy(s ->
                assertThat(s.getSuggestedCut()).isLessThanOrEqualTo(s.getCurrentSpend() * 0.40 + 0.01));
    }

    @Test
    void advise_withNoExpensesThisMonth_returnsEmptyPlan() {
        BudgetAdviceResponse res = advisor.advise(1000, 100);

        assertThat(res.getCurrentSpend()).isEqualTo(0.0);
        assertThat(res.getSuggestions()).isEmpty();
        assertThat(res.getSummary()).containsIgnoringCase("no expenses");
        mockServer.verify(); // no LLM call made
    }
}
