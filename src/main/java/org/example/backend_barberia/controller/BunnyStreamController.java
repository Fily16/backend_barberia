package org.example.backend_barberia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.dto.bunny.*;
import org.example.backend_barberia.service.BunnyStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/bunny")
@RequiredArgsConstructor
@Tag(name = "Bunny Stream", description = "Gestión de videos con Bunny Stream")
public class BunnyStreamController {

    private final BunnyStreamService bunnyStreamService;

    @GetMapping("/videos")
    @Operation(summary = "Listar videos", description = "Obtiene la lista paginada de videos de Bunny Stream")
    public ResponseEntity<BunnyVideoListResponse> listVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage) {
        return ResponseEntity.ok(bunnyStreamService.listVideos(page, itemsPerPage));
    }

    @GetMapping("/videos/{videoId}")
    @Operation(summary = "Obtener video", description = "Obtiene los detalles de un video específico")
    public ResponseEntity<BunnyVideoResponse> getVideo(@PathVariable String videoId) {
        return ResponseEntity.ok(bunnyStreamService.getVideo(videoId));
    }

    @PostMapping("/videos")
    @Operation(summary = "Crear video", description = "Crea un nuevo video y retorna la URL para subir el archivo")
    public ResponseEntity<BunnyUploadUrlResponse> createVideo(@RequestParam String title) {
        return ResponseEntity.ok(bunnyStreamService.createVideoAndGetUploadUrl(title));
    }

    @PutMapping("/videos/{videoId}")
    @Operation(summary = "Actualizar video", description = "Actualiza el título de un video")
    public ResponseEntity<BunnyVideoResponse> updateVideo(
            @PathVariable String videoId,
            @RequestParam String title) {
        return ResponseEntity.ok(bunnyStreamService.updateVideo(videoId, title));
    }

    @DeleteMapping("/videos/{videoId}")
    @Operation(summary = "Eliminar video", description = "Elimina un video de Bunny Stream")
    public ResponseEntity<Void> deleteVideo(@PathVariable String videoId) {
        bunnyStreamService.deleteVideo(videoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/videos/{videoId}/urls")
    @Operation(summary = "Obtener URLs", description = "Obtiene las URLs de reproducción de un video")
    public ResponseEntity<BunnyStreamService.VideoUrlsDto> getVideoUrls(@PathVariable String videoId) {
        return ResponseEntity.ok(bunnyStreamService.getVideoUrls(videoId));
    }

    @PostMapping("/videos/{videoId}/thumbnail")
    @Operation(summary = "Subir thumbnail", description = "Sube un thumbnail personalizado para un video")
    public ResponseEntity<Map<String, String>> uploadThumbnail(
            @PathVariable String videoId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato no válido. Usa JPG, PNG o WebP"));
        }
        
        // Convertir a Base64 data URL para almacenar directamente
        String base64Image = java.util.Base64.getEncoder().encodeToString(file.getBytes());
        String dataUrl = "data:" + contentType + ";base64," + base64Image;
        
        return ResponseEntity.ok(Map.of("thumbnailUrl", dataUrl));
    }
}
