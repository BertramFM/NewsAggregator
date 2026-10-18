package app.services;

import java.time.LocalDate;

public class GuardianUrlBuilder {
    private final String baseUrl = "https://content.guardianapis.com/search?";
    private final String apiKey = System.getenv("GUARDIAN_API_KEY");
    private static final int pageSize = 50;

    // https://content.guardianapis.com/search?section=sport&from-date=2026-10-10&show-fields=bodyText&api-key=
    public String buildURLFromDateTopic(GuardianTopic topic, LocalDate fromDate, int page) {
        return baseUrl
                + "section=" + topic.getSectionId()
                + "&from-date=" + fromDate
                + "&page-size=" + pageSize
                + "&page=" + page
                + "&show-fields=bodyText"
                + "&api-key=" + apiKey;
    }
}
