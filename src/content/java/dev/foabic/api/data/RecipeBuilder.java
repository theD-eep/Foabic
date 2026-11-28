package dev.foabic.api.data;

import dev.foabic.api.data.internal.FoabicDataContextImpl;
import dev.foabic.api.util.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A small recipe writer for the common survival recipes.
 *
 * <pre>{@code
 * context.recipe(id("feather_blade"))
 *        .shaped(" F ", " F ", " S ")
 *        .define('F', "minecraft:feather")
 *        .define('S', "minecraft:stick")
 *        .result("mymod:feather_blade")
 *        .save();
 * }</pre>
 */
public final class RecipeBuilder {

    private final FoabicDataContextImpl owner;
    private final ResourceLocation id;
    private String type = "minecraft:crafting_shaped";
    private String group;
    private String category;
    private List<String> pattern;
    private final Map<Character, String> keys = new LinkedHashMap<>();
    private List<String> ingredients;
    private String result;
    private int count = 1;
    private float experience;
    private int cookingTime = 200;

    public RecipeBuilder(FoabicDataContextImpl owner, ResourceLocation id) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.id = Objects.requireNonNull(id, "id");
    }

    /** Sets a shaped recipe pattern. Spaces are empty slots. */
    public RecipeBuilder shaped(String... pattern) {
        this.type = "minecraft:crafting_shaped";
        this.pattern = List.of(pattern);
        this.ingredients = null;
        return this;
    }

    /** Maps a pattern character to an item id or a {@code #tag}. */
    public RecipeBuilder define(char symbol, String ingredient) {
        keys.put(symbol, ingredient(ingredient));
        return this;
    }

    /** Sets a shapeless recipe. */
    public RecipeBuilder shapeless(String... ingredients) {
        this.type = "minecraft:crafting_shapeless";
        this.ingredients = new ArrayList<>();
        for (String ingredient : ingredients) {
            this.ingredients.add(ingredient(ingredient));
        }
        this.pattern = null;
        return this;
    }

    public RecipeBuilder smelting(String ingredient, String result, float experience) {
        return cooking("minecraft:smelting", ingredient, result, experience, 200);
    }

    public RecipeBuilder blasting(String ingredient, String result, float experience) {
        return cooking("minecraft:blasting", ingredient, result, experience, 100);
    }

    public RecipeBuilder smoking(String ingredient, String result, float experience) {
        return cooking("minecraft:smoking", ingredient, result, experience, 100);
    }

    public RecipeBuilder campfire(String ingredient, String result, float experience) {
        return cooking("minecraft:campfire_cooking", ingredient, result, experience, 600);
    }

    public RecipeBuilder stonecutting(String ingredient, String result, int count) {
        this.type = "minecraft:stonecutting";
        this.ingredients = List.of(ingredient(ingredient));
        this.result = item(result);
        this.count = count;
        this.pattern = null;
        return this;
    }

    public RecipeBuilder cooking(String type, String ingredient, String result,
                                 float experience, int cookingTime) {
        this.type = Objects.requireNonNull(type, "type");
        this.ingredients = List.of(ingredient(ingredient));
        this.result = item(result);
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.pattern = null;
        return this;
    }

    public RecipeBuilder result(String item) {
        return result(item, 1);
    }

    public RecipeBuilder result(String item, int count) {
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("recipe result count must be 1..99");
        }
        this.result = item(item);
        this.count = count;
        return this;
    }

    public RecipeBuilder group(String group) {
        this.group = blankToNull(group);
        return this;
    }

    public RecipeBuilder category(String category) {
        this.category = blankToNull(category);
        return this;
    }

    public ResourceLocation id() {
        return id;
    }

    /** Writes the recipe file. */
    public void save() {
        if (result == null) throw new IllegalStateException("recipe " + id + " has no result");
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", type);
        if (group != null) json.put("group", group);
        if (category != null) json.put("category", category);

        if (type.equals("minecraft:crafting_shaped")) {
            if (pattern == null || pattern.isEmpty()) {
                throw new IllegalStateException("shaped recipe " + id + " has no pattern");
            }
            if (pattern.size() > 3) {
                throw new IllegalStateException("shaped recipe " + id + " has more than 3 rows");
            }
            for (String row : pattern) {
                if (row.length() > 3) {
                    throw new IllegalStateException("shaped recipe " + id
                            + " has a row longer than 3 columns");
                }
                for (char symbol : row.toCharArray()) {
                    if (symbol != ' ' && !keys.containsKey(symbol)) {
                        throw new IllegalStateException("recipe " + id
                                + " uses undefined pattern key '" + symbol + "'");
                    }
                }
            }
            Map<String, String> keyJson = new LinkedHashMap<>();
            keys.forEach((symbol, value) -> keyJson.put(String.valueOf(symbol), value));
            json.put("pattern", pattern);
            json.put("key", keyJson);
        } else if (type.equals("minecraft:crafting_shapeless")) {
            if (ingredients == null || ingredients.isEmpty()) {
                throw new IllegalStateException("shapeless recipe " + id + " has no ingredients");
            }
            json.put("ingredients", ingredients);
        } else if (type.equals("minecraft:stonecutting")) {
            json.put("ingredient", ingredients.getFirst());
            json.put("count", count);
        } else {
            json.put("ingredient", ingredients.getFirst());
            json.put("experience", experience);
            json.put("cookingtime", cookingTime);
        }

        Map<String, Object> resultJson = new LinkedHashMap<>();
        resultJson.put("id", result);
        if (!type.equals("minecraft:stonecutting") && count != 1) {
            resultJson.put("count", count);
        }
        json.put("result", resultJson);

        owner.put("data/" + id.namespace() + "/recipe/" + id.path() + ".json", json);
    }

    private static String ingredient(String value) {
        Objects.requireNonNull(value, "ingredient");
        String text = value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("ingredient must not be blank");
        if (text.startsWith("#")) return "#" + ResourceLocation.parse(text.substring(1));
        return ResourceLocation.parse(text).toString();
    }

    private static String item(String value) {
        Objects.requireNonNull(value, "item");
        String text = value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("result item must not be blank");
        return ResourceLocation.parse(text).toString();
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
