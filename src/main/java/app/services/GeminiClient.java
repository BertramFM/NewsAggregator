package app.services;

import app.exceptions.ApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class GeminiClient implements AiClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String MODEL = "gemini-3.5-flash-lite";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String endpoint = BASE_URL + MODEL + ":generateContent";

    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public GeminiClient() {
        this.apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not set");
        }
    }

    @Override
    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new ApiException(400, "Prompt is required");
        }

        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                throw new ApiException(429, "Gemini rate limit reached");
            }

            if (response.statusCode() != 200) {
                log.error("Gemini request failed | Status code: {}", response.statusCode());
                throw new ApiException(502, "Gemini request failed with status " + response.statusCode());
            }

            GeminiResponse parsed = objectMapper.readValue(response.body(), GeminiResponse.class);
            if (parsed.candidates() == null || parsed.candidates().isEmpty()
                    || parsed.candidates().getFirst().content() == null
                    || parsed.candidates().getFirst().content().parts() == null
                    || parsed.candidates().getFirst().content().parts().isEmpty()
                    || parsed.candidates().getFirst().content().parts().getFirst().text() == null) {
                throw new ApiException(502, "Gemini returned no text");
            }
            return parsed.candidates().getFirst().content().parts().getFirst().text();

        } catch (ApiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(500, "Gemini request interrupted");
        } catch (Exception e) {
            throw new ApiException(502, "Gemini request failed: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}
}
