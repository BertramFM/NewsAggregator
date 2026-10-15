package app.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianResponseDTO {
    @JsonProperty
    private String status;
    @JsonProperty
    private int total;
    @JsonProperty
    private int pageSize;
    @JsonProperty
    private int currentPage;
    @JsonProperty
    private int pages;
    @JsonProperty
    List<GuardianArticleDTO> results;
}

