package dev.foabic.test;

import dev.foabic.api.data.FoabicDataContext;
import dev.foabic.api.data.TagType;
import dev.foabic.api.data.internal.FoabicDataContextImpl;
import dev.foabic.api.util.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Platform-neutral tests for the v1.3 data/tag/recipe generator. */
final class DataGenerationTests {

    private DataGenerationTests() {
    }

    static void run() {
        Path root = tempDir();
        FoabicDataContextImpl data = new FoabicDataContextImpl("demo");
        FoabicDataContext context = data;

        ResourceLocation block = ResourceLocation.of("demo", "feather_collector");
        ResourceLocation blade = ResourceLocation.of("demo", "feather_blade");
        context.blockStates(block).simple("demo:block/feather_collector");
        context.itemModel(blade, "minecraft:item/handheld",
                Map.of("layer0", "demo:item/feather_blade"));
        context.recipe(block)
                .shaped("F F", "FCF", "FFF")
                .define('F', "minecraft:feather")
                .define('C', "minecraft:chest")
                .result(block.toString())
                .save();
        context.tag(TagType.ITEM, ResourceLocation.parse("minecraft:swords"))
                .add(blade)
                .optional("demo:missing_but_optional");
        context.tag(TagType.BLOCK, ResourceLocation.parse("minecraft:mineable/axe"))
                .add(block);
        context.lootTable(block).dropsSelf();
        context.lang("en_us").put("block.demo.feather_collector", "Feather Collector");

        int written = data.write(root);
        check(written == 9, "unexpected generated file count: " + written);
        check(Files.isRegularFile(root.resolve(
                "assets/demo/blockstates/feather_collector.json")), "missing blockstate");
        check(Files.isRegularFile(root.resolve(
                "assets/demo/models/item/feather_blade.json")), "missing item model");
        check(Files.isRegularFile(root.resolve(
                "data/demo/recipe/feather_collector.json")), "missing recipe");
        check(Files.isRegularFile(root.resolve(
                "data/minecraft/tags/item/swords.json")), "missing item tag");
        check(Files.isRegularFile(root.resolve(
                "data/minecraft/tags/block/mineable/axe.json")), "missing block tag");
        check(Files.isRegularFile(root.resolve(
                "data/demo/loot_table/blocks/feather_collector.json")), "missing loot table");
        check(Files.isRegularFile(root.resolve(
                "assets/demo/lang/en_us.json")), "missing language file");

        String recipe = read(root.resolve("data/demo/recipe/feather_collector.json"));
        check(recipe.contains("\"minecraft:crafting_shaped\""), "recipe type");
        check(recipe.contains("\"minecraft:feather\""), "recipe ingredient");
        check(recipe.contains("\"id\":\"demo:feather_collector\""), "recipe result");

        String tag = read(root.resolve("data/minecraft/tags/item/swords.json"));
        check(tag.contains("\"demo:feather_blade\""), "tag item");
        check(tag.contains("\"required\":false"), "optional tag entry");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("foabic-data-test-");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
