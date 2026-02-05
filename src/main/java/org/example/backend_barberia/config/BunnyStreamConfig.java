package org.example.backend_barberia.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "bunny.stream")
@Getter
@Setter
public class BunnyStreamConfig {

    private String apiKey;         // API Key para llamadas a la API de Stream
    private String authKey;        // Token Authentication Key para signatures de upload
    private String accountApiKey;  // Account API Key para estadísticas de almacenamiento
    private String libraryId;
    private String cdnHostname;
    private String apiUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Genera la URL base para la API de Bunny Stream
     */
    public String getLibraryApiUrl() {
        return apiUrl + "/library/" + libraryId;
    }

    /**
     * Genera la URL de embed para un video
     */
    public String getEmbedUrl(String videoId) {
        return "https://iframe.mediadelivery.net/embed/" + libraryId + "/" + videoId;
    }

    /**
     * Genera la URL directa del video (para reproductor personalizado)
     */
    public String getDirectPlayUrl(String videoId) {
        return "https://" + cdnHostname + "/" + videoId + "/play_720p.mp4";
    }

    /**
     * Genera la URL del thumbnail
     */
    public String getThumbnailUrl(String videoId) {
        return "https://" + cdnHostname + "/" + videoId + "/thumbnail.jpg";
    }

    /**
     * Genera la URL del playlist HLS (para streaming adaptativo)
     */
    public String getHlsPlaylistUrl(String videoId) {
        return "https://" + cdnHostname + "/" + videoId + "/playlist.m3u8";
    }
}
