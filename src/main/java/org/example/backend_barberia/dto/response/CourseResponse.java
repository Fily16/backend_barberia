package org.example.backend_barberia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    
    private Long id;
    private String slug;
    private String title;
    private String description;
    private String thumbnailUrl;
    private Boolean active;
    private Integer orderIndex;
    private Integer totalVideos;
    private List<VideoResponse> theoryVideos;
    private List<VideoResponse> practiceVideos;
}
