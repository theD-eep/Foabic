package dev.foabic.api.data;

import dev.foabic.api.data.internal.FoabicDataContextImpl;
import dev.foabic.api.util.ResourceLocation;

import java.util.Objects;

/**
 * Adds entries to a tag. Entries merge across calls and providers.
 *
 * <pre>{@code
 * context.tag(TagType.ITEM, ResourceLocation.parse("minecraft:swords"))
 *        .add("mymod:feather_blade");
 * }</pre>
 */
public final class TagBuilder {

    private final FoabicDataContextImpl owner;
    private final TagType type;
    private final ResourceLocation tag;

    public TagBuilder(FoabicDataContextImpl owner, TagType type, ResourceLocation tag) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.type = Objects.requireNonNull(type, "type");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    /** Adds a required item/block/... id. */
    public TagBuilder add(String id) {
        owner.addTag(type, tag, value(id), true, false);
        return this;
    }

    public TagBuilder add(ResourceLocation id) {
        return add(Objects.requireNonNull(id, "id").toString());
    }

    /** Adds another tag by id, producing {@code #namespace:path}. */
    public TagBuilder addTag(String id) {
        String value = value(id);
        if (!value.startsWith("#")) value = "#" + value;
        owner.addTag(type, tag, value, true, false);
        return this;
    }

    public TagBuilder addTag(ResourceLocation id) {
        return addTag(Objects.requireNonNull(id, "id").toString());
    }

    /** Adds an entry that is ignored when the referenced object does not exist. */
    public TagBuilder optional(String id) {
        owner.addTag(type, tag, value(id), false, false);
        return this;
    }

    /** Makes this tag replace the platform tag instead of appending to it. */
    public TagBuilder replace() {
        owner.addTag(type, tag, "", true, true);
        return this;
    }

    public ResourceLocation tag() {
        return tag;
    }

    public TagType type() {
        return type;
    }

    private static String value(String id) {
        Objects.requireNonNull(id, "id");
        String text = id.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("tag entry must not be blank");
        if (text.startsWith("#")) return "#" + ResourceLocation.parse(text.substring(1));
        return ResourceLocation.parse(text).toString();
    }
}
