package models;

import io.ebean.Model;
import io.ebean.annotation.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Preset entity mapped to the existing Supabase "presets" table.
 *
 * Stores IME indicator clock configuration presets including
 * clock display settings and per-language indicator colors.
 */
@Entity
@Table(name = "presets")
public class Preset extends Model {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String name;

    /**
     * JSONB column containing clock and indicator configuration.
     * Structure includes: clock mode, colors, fonts, window dimensions,
     * date/time formats, and per-language indicator settings.
     */
    @Column(nullable = false, columnDefinition = "jsonb")
    @DbJsonB
    private String settings;

    @Column(name = "share_code", unique = true)
    private String shareCode;

    @Column(name = "likes_count")
    private int likesCount = 0;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @Column(name = "created_at")
    @WhenCreated
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    @WhenModified
    private OffsetDateTime updatedAt;

    // --- Constructors ---

    public Preset() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
