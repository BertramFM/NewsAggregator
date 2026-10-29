package app.services;

@FunctionalInterface
public interface AiClient {
    public abstract String generateText(String prompt);

    default String generateJson(String prompt) {
        return generateText(prompt);
    }
}
