package org.example.backend_barberia.dto.bunny;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BunnyUploadUrlResponse {

    private String videoId;
    private String uploadUrl;
    private String libraryId;
    private String authorizationSignature;
    private Long authorizationExpire;
    
    // URLs que estarán disponibles después del upload
    private String embedUrl;
    private String thumbnailUrl;
    private String directPlayUrl;
    private String hlsPlaylistUrl;
}
