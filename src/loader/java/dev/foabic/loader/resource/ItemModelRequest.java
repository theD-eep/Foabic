package dev.foabic.loader.resource;

import dev.foabic.api.util.ResourceLocation;

import java.util.Objects;

/** An explicit item-model override from a mod. */
public record ItemModelRequest(ResourceLocation item, ResourceLocation model) {

    public ItemModelRequest {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(model, "model");
    }
}
