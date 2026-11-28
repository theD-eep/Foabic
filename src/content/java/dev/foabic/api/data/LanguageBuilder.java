package dev.foabic.api.data;

import dev.foabic.api.data.internal.FoabicDataContextImpl;

import java.util.Map;
import java.util.Objects;

/**
 * Writes translation keys into {@code assets/<modid>/lang/<locale>.json}.
 *
 * <pre>{@code
 * context.lang("en_us")
 *        .put("block.mymod.generator", "Feather Generator")
 *        .put("item.mymod.feather_blade", "Feather Blade");
 * }</pre>
 */
public final class LanguageBuilder {

    private final FoabicDataContextImpl owner;
    private final String locale;

    public LanguageBuilder(FoabicDataContextImpl owner, String locale) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    public LanguageBuilder put(String key, String value) {
        owner.addLanguage(locale, key, value);
        return this;
    }

    public LanguageBuilder putAll(Map<String, String> entries) {
        Objects.requireNonNull(entries, "entries");
        entries.forEach(this::put);
        return this;
    }

    public String locale() {
        return locale;
    }
}
