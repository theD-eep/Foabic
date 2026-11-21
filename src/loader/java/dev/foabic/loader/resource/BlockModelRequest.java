package dev.foabic.loader.resource;

import dev.foabic.api.util.ResourceLocation;

import java.util.Objects;

/** An explicit one-line block model request from a mod. */
public record BlockModelRequest(ResourceLocation block, String texture, String modelParent) {

    public BlockModelRequest {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(modelParent, "modelParent");
        if (texture.isBlank()) throw new IllegalArgumentException("texture must not be blank");
        if (modelParent.isBlank()) throw new IllegalArgumentException("modelParent must not be blank");
    }
}
