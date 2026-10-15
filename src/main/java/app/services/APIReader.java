package app.services;

import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APIReader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public <T> T get(String url, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200) {
                throw new RuntimeException("GET request failed. Status code: " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), responseType);

        } catch (Exception e) {
            throw new RuntimeException("API Request failed", e);
        }
    }
}
