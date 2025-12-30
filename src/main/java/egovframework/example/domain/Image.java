package egovframework.example.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "IMAGE",
    indexes = {
        @Index(name = "idx_image_place", columnList = "place_id")
    }
)
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private TourPlace place;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // getters/setters ...
    public Long getImageId() { return imageId; }
    public TourPlace getPlace() { return place; }
    public void setPlace(TourPlace place) { this.place = place; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

