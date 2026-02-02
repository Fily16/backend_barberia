package org.example.backend_barberia.dto.bunny;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BunnyVideoListResponse {

    @JsonProperty("totalItems")
    private Integer totalItems;

    @JsonProperty("currentPage")
    private Integer currentPage;

    @JsonProperty("itemsPerPage")
    private Integer itemsPerPage;

    @JsonProperty("items")
    private List<BunnyVideoResponse> items;
}
