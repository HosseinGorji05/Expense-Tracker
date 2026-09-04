package com.hosseingorji.expensetracker.service;

import com.hosseingorji.expensetracker.dto.BudgetAdviceResponse;
import com.hosseingorji.expensetracker.dto.BudgetAdviceResponse.CategorySpend;
import com.hosseingorji.expensetracker.dto.BudgetAdviceResponse.CutSuggestion;
import com.hosseingorji.expensetracker.model.Expense;
import com.hosseingorji.expensetracker.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Looks at this month's expenses and suggests where to cut spending to hit a
 * savings target. Asks the LLM first; if that is unavailable or unusable it
 * falls back to a deterministic "trim the biggest discretionary categories" rule.
 */
@Service
public class BudgetAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(BudgetAdvisorService.class);

    /** Categories we try to protect from cuts in the fallback heuristic. */
    private static final List<String> ESSENTIALS = List.of("Housing", "Utilities", "Health");

    private final ExpenseRepository repository;
    private final GroqClient groq;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BudgetAdvisorService(ExpenseRepository repository, GroqClient groq) {
        this.repository = repository;
        this.groq = groq;
    }

    public BudgetAdviceResponse advise(double monthlyBudget, double cutTarget) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);

        Map<String, Double> byCategory = new LinkedHashMap<>();
        for (Expense e : repository.findAll()) {
            if (e.getDate() != null && !e.getDate().isBefore(monthStart)) {
                byCategory.merge(e.getCategory(), e.getAmount(), Double::sum);
            }
        }
        // Largest category first — nicer for both the prompt and the UI.
        Map<String, Double> sorted = new LinkedHashMap<>();
        byCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(en -> sorted.put(en.getKey(), round(en.getValue())));

        double currentSpend = round(sorted.values().stream().mapToDouble(Double::doubleValue).sum());
        double overBudgetBy = round(Math.max(0, currentSpend - monthlyBudget));

        List<CutSuggestion> suggestions = askLlm(sorted, monthlyBudget, cutTarget, currentSpend);
        boolean aiGenerated = !suggestions.isEmpty();
        if (!aiGenerated) {
            suggestions = heuristic(sorted, cutTarget);
        }

        double totalCuts = round(suggestions.stream().mapToDouble(CutSuggestion::getSuggestedCut).sum());

        BudgetAdviceResponse res = new BudgetAdviceResponse();
        res.setMonthlyBudget(round(monthlyBudget));
        res.setCutTarget(round(cutTarget));
        res.setCurrentSpend(currentSpend);
        res.setOverBudgetBy(overBudgetBy);
        res.setTotalSuggestedCuts(totalCuts);
        res.setProjectedSpendAfterCuts(round(currentSpend - totalCuts));
        res.setAiGenerated(aiGenerated);
        res.setBreakdown(sorted.entrySet().stream()
                .map(en -> new CategorySpend(en.getKey(), en.getValue()))
                .toList());
        res.setSuggestions(suggestions);
        res.setSummary(buildSummary(currentSpend, monthlyBudget, cutTarget, totalCuts, sorted.isEmpty()));
        return res;
    }

    // --- LLM path ---

    private List<CutSuggestion> askLlm(Map<String, Double> byCategory, double budget,
                                      double cutTarget, double currentSpend) {
        if (!groq.isConfigured() || byCategory.isEmpty() || cutTarget <= 0) {
            return List.of();
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a personal budgeting assistant. This month's spending by category:\n");
        byCategory.forEach((cat, amt) -> prompt.append("- ").append(cat)
                .append(": $").append(String.format("%.2f", amt)).append('\n'));
        prompt.append("Total spent: $").append(String.format("%.2f", currentSpend)).append('\n');
        prompt.append("Monthly budget: $").append(String.format("%.2f", budget)).append('\n');
        prompt.append("The user wants to cut about $").append(String.format("%.2f", cutTarget))
                .append(" from their spending.\n\n");
        prompt.append("Suggest which categories to reduce and by how much. Rules:\n");
        prompt.append("- Only use categories from the list above.\n");
        prompt.append("- The suggested cuts should add up to roughly the cut target.\n");
        prompt.append("- Never suggest cutting more than the current spend in a category.\n");
        prompt.append("- Prefer discretionary categories (Entertainment, Shopping, Food) over essentials (Housing, Utilities, Health).\n\n");
        prompt.append("Respond with ONLY a JSON object, no prose, in exactly this shape:\n");
        prompt.append("{\"suggestions\":[{\"category\":\"Entertainment\",\"suggestedCut\":40.00,\"reason\":\"short reason\"}]}");

        String raw = groq.chat(prompt.toString(), 500);
        return parseSuggestions(raw, byCategory);
    }

    private List<CutSuggestion> parseSuggestions(String raw, Map<String, Double> byCategory) {
        List<CutSuggestion> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            log.warn("Budget advice response contained no JSON object");
            return out;
        }
        try {
            JsonNode arr = objectMapper.readTree(raw.substring(start, end + 1)).path("suggestions");
            for (JsonNode node : arr) {
                String category = matchCategory(node.path("category").asString(""), byCategory);
                if (category == null) {
                    continue;
                }
                double available = byCategory.get(category);
                double cut = round(Math.max(0, Math.min(available, node.path("suggestedCut").asDouble(0))));
                if (cut <= 0) {
                    continue;
                }
                String reason = node.path("reason").asString("").trim();
                out.add(new CutSuggestion(category, available, cut, reason.isBlank() ? "Suggested reduction" : reason));
            }
        } catch (Exception e) {
            log.warn("Could not parse budget advice JSON: {}", e.getMessage());
            return List.of();
        }
        return out;
    }

    /** Map whatever the model wrote back to an actual category key (case-insensitive). */
    private String matchCategory(String candidate, Map<String, Double> byCategory) {
        if (candidate == null) {
            return null;
        }
        for (String key : byCategory.keySet()) {
            if (key.equalsIgnoreCase(candidate.trim())) {
                return key;
            }
        }
        return null;
    }

    // --- deterministic fallback ---

    private List<CutSuggestion> heuristic(Map<String, Double> byCategory, double cutTarget) {
        List<CutSuggestion> out = new ArrayList<>();
        if (byCategory.isEmpty() || cutTarget <= 0) {
            return out;
        }
        // Prefer trimming discretionary categories, largest first; cap each cut at 40% of its spend.
        List<Map.Entry<String, Double>> ordered = byCategory.entrySet().stream()
                .sorted((a, b) -> {
                    boolean aEss = ESSENTIALS.contains(a.getKey());
                    boolean bEss = ESSENTIALS.contains(b.getKey());
                    if (aEss != bEss) {
                        return aEss ? 1 : -1;
                    }
                    return Double.compare(b.getValue(), a.getValue());
                })
                .toList();

        double remaining = cutTarget;
        for (Map.Entry<String, Double> en : ordered) {
            if (remaining <= 0.01) {
                break;
            }
            double cap = en.getValue() * 0.40;
            double cut = round(Math.min(cap, remaining));
            if (cut <= 0) {
                continue;
            }
            remaining -= cut;
            String reason = ESSENTIALS.contains(en.getKey())
                    ? "Essential category — trim only if unavoidable"
                    : "Largest discretionary category with room to reduce";
            out.add(new CutSuggestion(en.getKey(), round(en.getValue()), cut, reason));
        }
        return out;
    }

    private String buildSummary(double currentSpend, double budget, double cutTarget,
                                double totalCuts, boolean noData) {
        if (noData) {
            return "No expenses recorded this month yet — add some to get cut suggestions.";
        }
        String budgetLine = currentSpend > budget
                ? String.format("You're $%.2f over your $%.2f budget this month.", currentSpend - budget, budget)
                : String.format("You're $%.2f under your $%.2f budget this month.", budget - currentSpend, budget);
        String cutLine = String.format(" The plan below trims $%.2f (target was $%.2f), leaving a projected spend of $%.2f.",
                totalCuts, cutTarget, currentSpend - totalCuts);
        return budgetLine + cutLine;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
