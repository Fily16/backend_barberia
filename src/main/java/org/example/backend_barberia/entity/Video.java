package org.example.backend_barberia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String videoUrl; // URL del video (YouTube embed, Vimeo, etc.)

    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    private String duration; // ej: "45:00"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoType type;

    @Builder.Default
    @Column(nullable = false)
    private Integer orderIndex = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
