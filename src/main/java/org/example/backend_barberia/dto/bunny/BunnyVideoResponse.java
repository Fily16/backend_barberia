package org.example.backend_barberia.dto.bunny;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BunnyVideoResponse {

    @JsonProperty("guid")
    private String guid;

    @JsonProperty("title")
    private String title;

    @JsonProperty("dateUploaded")
    private String dateUploaded;

    @JsonProperty("views")
    private Long views;

    @JsonProperty("isPublic")
    private Boolean isPublic;

    @JsonProperty("length")
    private Integer length; // Duración en segundos

    @JsonProperty("status")
    private Integer status; // 0=created, 1=uploaded, 2=processing, 3=transcoding, 4=finished, 5=error

    @JsonProperty("framerate")
    private Double framerate;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("availableResolutions")
    private String availableResolutions;

    @JsonProperty("thumbnailCount")
    private Integer thumbnailCount;

    @JsonProperty("encodeProgress")
    private Integer encodeProgress;

    @JsonProperty("storageSize")
    private Long storageSize;

    @JsonProperty("captions")
    private Object captions;

    @JsonProperty("hasMP4Fallback")
    private Boolean hasMP4Fallback;

    @JsonProperty("collectionId")
    private String collectionId;

    @JsonProperty("thumbnailFileName")
    private String thumbnailFileName;

    @JsonProperty("averageWatchTime")
    private Integer averageWatchTime;

    @JsonProperty("totalWatchTime")
    private Long totalWatchTime;

    @JsonProperty("category")
    private String category;

    @JsonProperty("chapters")
    private Object chapters;

    @JsonProperty("moments")
    private Object moments;

    @JsonProperty("metaTags")
    private Object metaTags;

    @JsonProperty("transcodingMessages")
    private Object transcodingMessages;

    // URLs generadas por el backend (no vienen de Bunny API)
    private String embedUrl;
    private String directPlayUrl;
    private String thumbnailUrl;
    private String hlsPlaylistUrl;

    /**
     * Convierte la duración de segundos a formato legible (HH:MM:SS)
     */
    public String getFormattedDuration() {
        if (length == null || length == 0) return "00:00";
        
        int hours = length / 3600;
        int minutes = (length % 3600) / 60;
        int seconds = length % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Obtiene el estado como texto legible
     */
    public String getStatusText() {
        if (status == null) return "Desconocido";
        return switch (status) {
            case 0 -> "Creado";
            case 1 -> "Subido";
            case 2 -> "Procesando";
            case 3 -> "Transcodificando";
            case 4 -> "Listo";
            case 5 -> "Error";
            case 6 -> "Subiendo";
            default -> "Desconocido";
        };
    }

    /**
     * Indica si el video está listo para reproducir
     */
    public boolean isReady() {
        return status != null && status == 4;
    }
}
