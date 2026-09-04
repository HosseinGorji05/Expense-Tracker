package com.hosseingorji.expensetracker;

import com.hosseingorji.expensetracker.service.CategorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;

@SpringBootTest(properties = {
        "groq.api.key=test-key",
        "groq.api.url=https://api.groq.com/openai/v1/chat/completions"
})
class CategorizationServiceTest {

    @Autowired
    private CategorizationService service;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void verify() {
        mockServer.verify();
    }

    private void stubGroqReply(String content) {
        String json = """
                {"choices":[{"message":{"content":"%s"}}]}
                """.formatted(content);
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void categorize_whenLlmReturnsAllowedCategory_returnsIt() {
        stubGroqReply("Food");

        assertThat(service.categorize("Uber Eats order, $23.50")).isEqualTo("Food");
    }

    @Test
    void categorize_isCaseInsensitiveAndTrimmed() {
        stubGroqReply("  transportation \\n");

        assertThat(service.categorize("Gas station fill-up")).isEqualTo("Transportation");
    }

    @Test
    void categorize_whenLlmReturnsUnknownText_fallsBackToOther() {
        stubGroqReply("Groceries and snacks");

        assertThat(service.categorize("Walmart run")).isEqualTo("Other");
    }

    @Test
    void categorize_whenApiFails_fallsBackToOther() {
        mockServer.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withServerError());

        assertThat(service.categorize("anything")).isEqualTo("Other");
    }
}
