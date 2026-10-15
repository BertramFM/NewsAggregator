package app.services;

import lombok.Getter;

@Getter
public enum GuardianTopic {
    SPORT( "sport"),
    POLITICS( "politics"),
    BUSINESS( "business"),
    TECHNOLOGY( "technology"),
    SCIENCE( "science"),
    WORLD( "world"),
    FOOTBALL( "football"),
    FILM( "film"),
    MUSIC( "music"),
    SOCIETY( "society"),
    UKNEWS( "uk-news"),
    USNEWS( "us-news");

    private final String sectionId;

    GuardianTopic(String sectionId) {
        this.sectionId = sectionId;
    }
}
