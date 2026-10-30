package app.config;

import app.utils.Utils;

public record TokenSettings(String secretKey, String issuer, String expireTimeMillis) {

    public static TokenSettings fromEnvironment() {
        return new TokenSettings(read("SECRET_KEY"), read("ISSUER"), read("TOKEN_EXPIRE_TIME"));
    }

    private static String read(String name) {
        String value = System.getenv("DEPLOYED") != null
                ? System.getenv(name) : Utils.getPropertyValue(name, "config.properties");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not set");
        }
        return value;
    }
}
