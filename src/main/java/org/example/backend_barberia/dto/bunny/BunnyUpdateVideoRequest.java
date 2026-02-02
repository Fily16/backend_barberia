package org.example.backend_barberia.dto.bunny;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BunnyUpdateVideoRequest {

    @JsonProperty("title")
    private String title;

    @JsonProperty("collectionId")
    private String collectionId;

    @JsonProperty("chapters")
    private Object chapters;

    @JsonProperty("moments")
    private Object moments;

    @JsonProperty("metaTags")
    private Object metaTags;
}
