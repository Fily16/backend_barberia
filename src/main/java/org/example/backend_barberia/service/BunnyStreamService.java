package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.config.BunnyStreamConfig;
import org.example.backend_barberia.dto.bunny.*;
import org.example.backend_barberia.exception.BadRequestException;
import org.example.backend_barberia.exception.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BunnyStreamService {

    private final BunnyStreamConfig bunnyConfig;
    private final RestTemplate restTemplate;

    /**
     * Obtiene los headers necesarios para autenticación con Bunny
     */
    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", bunnyConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * Lista todos los videos de la biblioteca
     */
    public BunnyVideoListResponse listVideos(int page, int itemsPerPage) {
        String url = bunnyConfig.getLibraryApiUrl() + "/videos?page=" + page + "&itemsPerPage=" + itemsPerPage;
        
        try {
            HttpEntity<Void> entity = new HttpEntity<>(getAuthHeaders());
            ResponseEntity<BunnyVideoListResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                BunnyVideoListResponse.class
            );
            
            BunnyVideoListResponse result = response.getBody();
            if (result != null && result.getItems() != null) {
                // Agregar URLs a cada video
                result.getItems().forEach(video -> enrichVideoWithUrls(video));
            }
            return result;
        } catch (HttpClientErrorException e) {
            log.error("Error listando videos de Bunny: {}", e.getMessage());
            throw new BadRequestException("Error al obtener videos de Bunny Stream: " + e.getMessage());
        }
    }

    /**
     * Obtiene un video específico por su ID
     */
    public BunnyVideoResponse getVideo(String videoId) {
        String url = bunnyConfig.getLibraryApiUrl() + "/videos/" + videoId;
        
        try {
            HttpEntity<Void> entity = new HttpEntity<>(getAuthHeaders());
            ResponseEntity<BunnyVideoResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                BunnyVideoResponse.class
            );
            
            BunnyVideoResponse video = response.getBody();
            if (video != null) {
                enrichVideoWithUrls(video);
            }
            return video;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Video no encontrado en Bunny: " + videoId);
        } catch (HttpClientErrorException e) {
            log.error("Error obteniendo video de Bunny: {}", e.getMessage());
            throw new BadRequestException("Error al obtener video de Bunny Stream: " + e.getMessage());
        }
    }

    /**
     * Crea un nuevo video y retorna la URL para subir el archivo
     */
    public BunnyUploadUrlResponse createVideoAndGetUploadUrl(String title) {
        String url = bunnyConfig.getLibraryApiUrl() + "/videos";
        
        BunnyCreateVideoRequest request = BunnyCreateVideoRequest.builder()
                .title(title)
                .build();
        
        try {
            HttpEntity<BunnyCreateVideoRequest> entity = new HttpEntity<>(request, getAuthHeaders());
            ResponseEntity<BunnyVideoResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                BunnyVideoResponse.class
            );
            
            BunnyVideoResponse video = response.getBody();
            if (video == null || video.getGuid() == null) {
                throw new BadRequestException("Error: No se recibió ID del video creado");
            }

            // Generar URL de upload con autenticación
            long expirationTime = System.currentTimeMillis() / 1000 + 3600; // 1 hora
            String uploadUrl = bunnyConfig.getApiUrl() + "/library/" + bunnyConfig.getLibraryId() 
                    + "/videos/" + video.getGuid();
            
            // Generar signature para upload directo desde frontend
            String signature = generateUploadSignature(video.getGuid(), expirationTime);
            
            return BunnyUploadUrlResponse.builder()
                    .videoId(video.getGuid())
                    .uploadUrl(uploadUrl)
                    .libraryId(bunnyConfig.getLibraryId())
                    .authorizationSignature(signature)
                    .authorizationExpire(expirationTime)
                    .embedUrl(bunnyConfig.getEmbedUrl(video.getGuid()))
                    .thumbnailUrl(bunnyConfig.getThumbnailUrl(video.getGuid()))
                    .directPlayUrl(bunnyConfig.getDirectPlayUrl(video.getGuid()))
                    .hlsPlaylistUrl(bunnyConfig.getHlsPlaylistUrl(video.getGuid()))
                    .build();
                    
        } catch (HttpClientErrorException e) {
            log.error("Error creando video en Bunny: {}", e.getMessage());
            throw new BadRequestException("Error al crear video en Bunny Stream: " + e.getMessage());
        }
    }

    /**
     * Actualiza los metadatos de un video
     */
    public BunnyVideoResponse updateVideo(String videoId, String title) {
        String url = bunnyConfig.getLibraryApiUrl() + "/videos/" + videoId;
        
        BunnyUpdateVideoRequest request = BunnyUpdateVideoRequest.builder()
                .title(title)
                .build();
        
        try {
            HttpEntity<BunnyUpdateVideoRequest> entity = new HttpEntity<>(request, getAuthHeaders());
            ResponseEntity<BunnyVideoResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                BunnyVideoResponse.class
            );
            
            BunnyVideoResponse video = response.getBody();
            if (video != null) {
                enrichVideoWithUrls(video);
            }
            return video;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Video no encontrado en Bunny: " + videoId);
        } catch (HttpClientErrorException e) {
            log.error("Error actualizando video en Bunny: {}", e.getMessage());
            throw new BadRequestException("Error al actualizar video en Bunny Stream: " + e.getMessage());
        }
    }

    /**
     * Elimina un video de Bunny Stream
     */
    public void deleteVideo(String videoId) {
        String url = bunnyConfig.getLibraryApiUrl() + "/videos/" + videoId;
        
        try {
            HttpEntity<Void> entity = new HttpEntity<>(getAuthHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Video eliminado de Bunny Stream: {}", videoId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Video no encontrado en Bunny para eliminar: {}", videoId);
            // No lanzamos excepción, el video ya no existe
        } catch (HttpClientErrorException e) {
            log.error("Error eliminando video de Bunny: {}", e.getMessage());
            throw new BadRequestException("Error al eliminar video de Bunny Stream: " + e.getMessage());
        }
    }

    /**
     * Sube un thumbnail personalizado para un video
     * @param videoId ID del video en Bunny
     * @param imageData Bytes de la imagen (JPG/PNG)
     * @param contentType Tipo de contenido (image/jpeg, image/png)
     * @return URL del thumbnail
     */
    public String uploadThumbnail(String videoId, byte[] imageData, String contentType) {
        // Bunny acepta thumbnails via POST con la imagen en el body
        // Endpoint: POST /library/{libraryId}/videos/{videoId}/thumbnail
        String url = bunnyConfig.getLibraryApiUrl() + "/videos/" + videoId + "/thumbnail";
        
        log.info("Subiendo thumbnail para video: {} - URL: {}", videoId, url);
        log.info("Content-Type: {}, Size: {} bytes", contentType, imageData.length);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("AccessKey", bunnyConfig.getApiKey());
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            HttpEntity<byte[]> entity = new HttpEntity<>(imageData, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            log.info("Thumbnail response status: {}", response.getStatusCode());
            log.info("Thumbnail response body: {}", response.getBody());
            log.info("Thumbnail subido exitosamente para video: {}", videoId);
            
            // Agregar timestamp para evitar cache
            return bunnyConfig.getThumbnailUrl(videoId) + "?v=" + System.currentTimeMillis();
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Video no encontrado: {}", videoId);
            throw new ResourceNotFoundException("Video no encontrado en Bunny: " + videoId);
        } catch (HttpClientErrorException e) {
            log.error("Error subiendo thumbnail a Bunny: {} - Response: {}", e.getMessage(), e.getResponseBodyAsString());
            throw new BadRequestException("Error al subir thumbnail: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado subiendo thumbnail: {}", e.getMessage(), e);
            throw new BadRequestException("Error al subir thumbnail: " + e.getMessage());
        }
    }

    /**
     * Obtiene las URLs de reproducción para un video
     */
    public VideoUrlsDto getVideoUrls(String videoId) {
        return VideoUrlsDto.builder()
                .videoId(videoId)
                .embedUrl(bunnyConfig.getEmbedUrl(videoId))
                .directPlayUrl(bunnyConfig.getDirectPlayUrl(videoId))
                .thumbnailUrl(bunnyConfig.getThumbnailUrl(videoId))
                .build();
    }

    /**
     * Enriquece el objeto video con las URLs de reproducción
     */
    private void enrichVideoWithUrls(BunnyVideoResponse video) {
        if (video == null || video.getGuid() == null) return;
        
        String videoId = video.getGuid();
        video.setEmbedUrl(bunnyConfig.getEmbedUrl(videoId));
        video.setDirectPlayUrl(bunnyConfig.getDirectPlayUrl(videoId));
        video.setThumbnailUrl(bunnyConfig.getThumbnailUrl(videoId));
        video.setHlsPlaylistUrl(bunnyConfig.getHlsPlaylistUrl(videoId));
    }

    /**
     * Genera el signature para upload directo desde el frontend
     * Esto permite que el frontend suba directamente a Bunny sin pasar por nuestro servidor
     * Formula: SHA256(library_id + api_key + expiration_time + video_id)
     */
    private String generateUploadSignature(String videoId, long expirationTime) {
        try {
            // Según documentación de Bunny: SHA256(library_id + api_key + expiration_time + video_id)
            String stringToSign = bunnyConfig.getLibraryId() + bunnyConfig.getApiKey() + expirationTime + videoId;
            
            log.info("=== BUNNY SIGNATURE DEBUG ===");
            log.info("LibraryId: {}", bunnyConfig.getLibraryId());
            log.info("ApiKey length: {}", bunnyConfig.getApiKey().length());
            log.info("ExpirationTime: {}", expirationTime);
            log.info("VideoId: {}", videoId);
            log.info("StringToSign: {}", stringToSign);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(stringToSign.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            log.info("Generated signature: {}", hexString.toString());
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generando signature", e);
        }
    }

    /**
     * Obtiene las estadísticas de almacenamiento de la biblioteca de videos
     * Usa la Account API Key (diferente a la Stream API Key)
     */
    public StorageStatsDto getStorageStats() {
        // Usar la API de Account de Bunny para obtener info de la biblioteca
        String url = "https://api.bunny.net/videolibrary/" + bunnyConfig.getLibraryId();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("AccessKey", bunnyConfig.getAccountApiKey()); // Usar Account API Key
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                java.util.Map.class
            );
            
            java.util.Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new BadRequestException("No se pudo obtener información de almacenamiento");
            }
            
            // Extraer datos de storage
            Long storageUsage = body.get("StorageUsage") != null ? 
                    ((Number) body.get("StorageUsage")).longValue() : 0L;
            Long trafficUsage = body.get("TrafficUsage") != null ? 
                    ((Number) body.get("TrafficUsage")).longValue() : 0L;
            Integer videoCount = body.get("VideoCount") != null ? 
                    ((Number) body.get("VideoCount")).intValue() : 0;
            String name = (String) body.get("Name");
            
            return StorageStatsDto.builder()
                    .libraryId(bunnyConfig.getLibraryId())
                    .libraryName(name)
                    .storageUsedBytes(storageUsage)
                    .storageUsedFormatted(formatBytes(storageUsage))
                    .trafficUsedBytes(trafficUsage)
                    .trafficUsedFormatted(formatBytes(trafficUsage))
                    .videoCount(videoCount)
                    .build();
                    
        } catch (HttpClientErrorException e) {
            log.error("Error obteniendo storage de Bunny Account API: {} - Response: {}", 
                    e.getMessage(), e.getResponseBodyAsString());
            throw new BadRequestException("Error al obtener estadísticas de almacenamiento: " + e.getMessage());
        }
    }
    
    /**
     * Formatea bytes a formato legible (KB, MB, GB)
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * DTO interno para URLs de video
     */
    @lombok.Data
    @lombok.Builder
    public static class VideoUrlsDto {
        private String videoId;
        private String embedUrl;
        private String directPlayUrl;
        private String thumbnailUrl;
    }
    
    /**
     * DTO para estadísticas de almacenamiento
     */
    @lombok.Data
    @lombok.Builder
    public static class StorageStatsDto {
        private String libraryId;
        private String libraryName;
        private Long storageUsedBytes;
        private String storageUsedFormatted;
        private Long trafficUsedBytes;
        private String trafficUsedFormatted;
        private Integer videoCount;
    }
}
