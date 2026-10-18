package app.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianArticleDTO {
    @JsonProperty
    private String id;
    @JsonProperty
    private String type;

    // Den her er topic
    @JsonProperty
    private String sectionId;

    @JsonProperty
    private String sectionName;


    @JsonProperty
    private OffsetDateTime webPublicationDate;
    @JsonProperty
    private String webTitle;
    @JsonProperty
    private String webUrl;
    @JsonProperty
    private String apiUrl;
    @JsonProperty
    private GuardianFieldsDTO fields;
}
