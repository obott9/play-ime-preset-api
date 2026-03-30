package models;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Like entity mapped to the existing Supabase "likes" table.
 *
 * Tracks which users have liked which presets.
 * Unique constraint on (user_id, preset_id) prevents duplicate likes.
 */
@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "preset_id"})
})
public class Like extends Model {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "preset_id", nullable = false)
    private Preset preset;

    @Column(name = "created_at")
    @WhenCreated
    private OffsetDateTime createdAt;

    // --- Constructors ---

    public Like() {
    }

    // --- Getters / Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Preset getPreset() {
        return preset;
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
