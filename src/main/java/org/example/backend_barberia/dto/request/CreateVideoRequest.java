package org.example.backend_barberia.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.backend_barberia.entity.VideoType;

@Data
public class CreateVideoRequest {
    
    @NotNull(message = "El ID del curso es requerido")
    private Long courseId;
    
    @NotBlank(message = "El título es requerido")
    private String title;
    
    private String description;
    
    @NotBlank(message = "La URL del video es requerida")
    private String videoUrl;
    
    private String thumbnailUrl;
    
    private String duration;
    
    @NotNull(message = "El tipo de video es requerido")
    private VideoType type;
    
    private Integer orderIndex = 0;
}
