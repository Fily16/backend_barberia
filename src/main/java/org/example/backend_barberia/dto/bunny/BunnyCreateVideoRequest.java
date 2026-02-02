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
public class BunnyCreateVideoRequest {

    @JsonProperty("title")
    private String title;

    @JsonProperty("collectionId")
    private String collectionId;

    @JsonProperty("thumbnailTime")
    private Integer thumbnailTime; // Tiempo en segundos para generar thumbnail
}
