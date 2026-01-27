package org.example.backend_barberia.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseRequest {
    
    @NotBlank(message = "El slug es requerido")
    private String slug;
    
    @NotBlank(message = "El título es requerido")
    private String title;
    
    private String description;
    
    private String thumbnailUrl;
    
    private Integer orderIndex = 0;
}
