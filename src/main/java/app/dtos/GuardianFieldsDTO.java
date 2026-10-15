package app.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianFieldsDTO {
    // Bruger "bodyText" fordi ingen html, hvis jeg gerne vil have html skal jeg bruge "body" (skal også ændres is GuardianApiBuilder)
    @JsonProperty
    private String bodyText;
}
