package com.hosseingorji.expensetracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategorizationService {

    private static final Logger log = LoggerFactory.getLogger(CategorizationService.class);

    public static final List<String> ALLOWED_CATEGORIES = List.of(
            "Food", "Transportation", "Entertainment", "Utilities",
            "Shopping", "Health", "Housing", "Other"
    );
    private static final String FALLBACK = "Other";

    private final GroqClient groq;

    public CategorizationService(GroqClient groq) {
        this.groq = groq;
    }

    /**
     * Ask the LLM to classify an expense description. Always returns one of
     * {@link #ALLOWED_CATEGORIES}; on any failure (no key, API down, unexpected
     * answer) it degrades gracefully to "Other".
     */
    public String categorize(String description) {
        String rawAnswer = groq.chat(buildPrompt(description), 100).trim();

        for (String category : ALLOWED_CATEGORIES) {
            if (rawAnswer.equalsIgnoreCase(category)) {
                return category;
            }
        }
        if (!rawAnswer.isBlank()) {
            log.warn("LLM returned an unrecognized category: '{}'", rawAnswer);
        }
        return FALLBACK;
    }

    private String buildPrompt(String description) {
        return "Classify this expense into EXACTLY ONE of these categories: "
                + String.join(", ", ALLOWED_CATEGORIES)
                + ". Respond with ONLY the category name, nothing else.\n\n"
                + "Expense: \"" + description + "\"";
    }
}
