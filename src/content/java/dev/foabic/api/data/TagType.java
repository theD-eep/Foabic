package dev.foabic.api.data;

/**
 * Standard tag directories. The directory is the path segment after
 * {@code data/<namespace>/tags/}.
 */
public enum TagType {

    ITEM("item"),
    BLOCK("block"),
    ENTITY_TYPE("entity_type"),
    FLUID("fluid"),
    GAME_EVENT("game_event"),
    BIOME("biome"),
    STRUCTURE("structure"),
    ENCHANTMENT("enchantment"),
    RECIPE("recipe"),
    FUNCTION("function"),
    DAMAGE_TYPE("damage_type");

    private final String directory;

    TagType(String directory) {
        this.directory = directory;
    }

    public String directory() {
        return directory;
    }
}
