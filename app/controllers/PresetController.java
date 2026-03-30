package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ebean.DB;
import io.ebean.PagedList;
import models.Like;
import models.Preset;
import play.libs.Json;
import play.mvc.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API controller for IME preset CRUD operations.
 *
 * Demonstrates Play Framework 3.0 + Java 21 patterns:
 * - Async CompletionStage-based actions
 * - JSONB handling with PostgreSQL
 * - Pagination, filtering, and sorting
 * - Unified error response format
 */
@Singleton
public class PresetController extends Controller {

    private static final Logger logger = LoggerFactory.getLogger(PresetController.class);

    @Inject
    public PresetController() {
    }

    // ========== CRUD ==========

    /**
     * GET /api/presets
     * List presets with pagination and optional default-only filter.
     */
    public CompletionStage<Result> list(Integer page, Integer size, Boolean defaultOnly) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Listing presets: page={}, size={}, defaultOnly={}", page, size, defaultOnly);

            var query = DB.find(Preset.class).orderBy("createdAt desc");

            if (Boolean.TRUE.equals(defaultOnly)) {
                query.where().eq("isDefault", true);
            }

            PagedList<Preset> pagedList = query
                    .setFirstRow(page * size)
                    .setMaxRows(size)
                    .findPagedList();

            ObjectNode response = Json.newObject();
            response.put("page", page);
            response.put("size", size);
            response.put("totalCount", pagedList.getTotalCount());
            response.put("totalPages", pagedList.getTotalPageCount());
            response.set("data", Json.toJson(pagedList.getList()));

            logger.info("Found {} presets (total: {})", pagedList.getList().size(), pagedList.getTotalCount());
            return ok(response);
        });
    }

    /**
     * GET /api/presets/:id
     * Get a single preset by UUID.
     */
    public CompletionStage<Result> get(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Getting preset: id={}", id);

            Preset preset = DB.find(Preset.class, id);
            if (preset == null) {
                logger.warn("Preset not found: id={}", id);
                return notFound(errorJson("Preset not found", id.toString()));
            }

            return ok(Json.toJson(preset));
        });
    }

    /**
     * POST /api/presets
     * Create a new preset.
     */
    public CompletionStage<Result> create(Http.Request request) {
        return CompletableFuture.supplyAsync(() -> {
            JsonNode body = request.body().asJson();
            if (body == null) {
                logger.warn("Create preset: empty request body");
                return badRequest(errorJson("Request body must be JSON", null));
            }

            // Validate required fields
            if (!body.has("name") || body.get("name").asText().isBlank()) {
                return badRequest(errorJson("Field 'name' is required", null));
            }
            if (!body.has("settings")) {
                return badRequest(errorJson("Field 'settings' is required", null));
            }

            Preset preset = new Preset();
            preset.setId(UUID.randomUUID());
            preset.setName(body.get("name").asText());
            preset.setSettings(body.get("settings").toString());

            if (body.has("user_id") && !body.get("user_id").isNull()) {
                preset.setUserId(UUID.fromString(body.get("user_id").asText()));
            }

            logger.info("Creating preset: name={}", preset.getName());
            DB.save(preset);
            logger.info("Preset created: id={}, name={}", preset.getId(), preset.getName());

            return created(Json.toJson(preset));
        });
    }

    /**
     * PUT /api/presets/:id
     * Update an existing preset.
     */
    public CompletionStage<Result> update(UUID id, Http.Request request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Updating preset: id={}", id);

            Preset preset = DB.find(Preset.class, id);
            if (preset == null) {
                logger.warn("Update failed - preset not found: id={}", id);
                return notFound(errorJson("Preset not found", id.toString()));
            }

            JsonNode body = request.body().asJson();
            if (body == null) {
                return badRequest(errorJson("Request body must be JSON", null));
            }

            if (body.has("name") && !body.get("name").asText().isBlank()) {
                preset.setName(body.get("name").asText());
            }
            if (body.has("settings")) {
                preset.setSettings(body.get("settings").toString());
            }

            DB.save(preset);
            logger.info("Preset updated: id={}, name={}", preset.getId(), preset.getName());

            return ok(Json.toJson(preset));
        });
    }

    /**
     * DELETE /api/presets/:id
     * Delete a preset.
     */
    public CompletionStage<Result> delete(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Deleting preset: id={}", id);

            Preset preset = DB.find(Preset.class, id);
            if (preset == null) {
                logger.warn("Delete failed - preset not found: id={}", id);
                return notFound(errorJson("Preset not found", id.toString()));
            }

            if (preset.isDefault()) {
                logger.warn("Delete refused - cannot delete default preset: id={}", id);
                return forbidden(errorJson("Cannot delete default presets", id.toString()));
            }

            DB.delete(preset);
            logger.info("Preset deleted: id={}", id);

            return noContent();
        });
    }

    // ========== Share ==========

    /**
     * GET /api/presets/shared/:code
     * Get a preset by its share code.
     */
    public CompletionStage<Result> getByShareCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Looking up preset by share code: {}", code);

            Preset preset = DB.find(Preset.class)
                    .where()
                    .eq("shareCode", code)
                    .findOne();

            if (preset == null) {
                logger.warn("Preset not found for share code: {}", code);
                return notFound(errorJson("Preset not found for share code", code));
            }

            return ok(Json.toJson(preset));
        });
    }

    // ========== Like ==========

    /**
     * POST /api/presets/:id/like
     * Toggle like on a preset. Requires user_id in request body.
     */
    public CompletionStage<Result> toggleLike(UUID id, Http.Request request) {
        return CompletableFuture.supplyAsync(() -> {
            JsonNode body = request.body().asJson();
            if (body == null || !body.has("user_id")) {
                return badRequest(errorJson("Field 'user_id' is required", null));
            }

            UUID userId = UUID.fromString(body.get("user_id").asText());
            logger.info("Toggle like: presetId={}, userId={}", id, userId);

            Preset preset = DB.find(Preset.class, id);
            if (preset == null) {
                return notFound(errorJson("Preset not found", id.toString()));
            }

            // Check existing like
            Optional<Like> existingLike = DB.find(Like.class)
                    .where()
                    .eq("userId", userId)
                    .eq("preset.id", id)
                    .findOneOrEmpty();

            ObjectNode response = Json.newObject();

            if (existingLike.isPresent()) {
                // Unlike
                DB.delete(existingLike.get());
                preset.setLikesCount(Math.max(0, preset.getLikesCount() - 1));
                DB.save(preset);
                response.put("action", "unliked");
                logger.info("Unliked: presetId={}, newCount={}", id, preset.getLikesCount());
            } else {
                // Like
                Like like = new Like();
                like.setId(UUID.randomUUID());
                like.setUserId(userId);
                like.setPreset(preset);
                DB.save(like);
                preset.setLikesCount(preset.getLikesCount() + 1);
                DB.save(preset);
                response.put("action", "liked");
                logger.info("Liked: presetId={}, newCount={}", id, preset.getLikesCount());
            }

            response.put("likesCount", preset.getLikesCount());
            return ok(response);
        });
    }

    // ========== Popular ==========

    /**
     * GET /api/presets/popular
     * List presets ordered by likes_count descending.
     */
    public CompletionStage<Result> popular(Integer limit) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Fetching popular presets: limit={}", limit);

            List<Preset> presets = DB.find(Preset.class)
                    .where()
                    .or()
                        .eq("isDefault", true)
                        .isNotNull("shareCode")
                    .endOr()
                    .orderBy("likesCount desc")
                    .setMaxRows(limit)
                    .findList();

            logger.info("Found {} popular presets", presets.size());
            return ok(Json.toJson(presets));
        });
    }

    // ========== Health ==========

    /**
     * GET /api/health
     * Health check endpoint.
     */
    public Result health() {
        ObjectNode response = Json.newObject();
        response.put("status", "ok");
        response.put("framework", "Play Framework 3.0.10");
        response.put("java", System.getProperty("java.version"));
        return ok(response);
    }

    // ========== Helpers ==========

    private static JsonNode errorJson(String message, String detail) {
        ObjectNode error = Json.newObject();
        error.put("error", message);
        if (detail != null) {
            error.put("detail", detail);
        }
        return error;
    }
}
