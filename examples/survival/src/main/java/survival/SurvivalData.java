package survival;

import dev.foabic.api.data.FoabicDataContext;
import dev.foabic.api.data.FoabicDataProvider;
import dev.foabic.api.data.TagType;
import dev.foabic.api.util.ResourceLocation;

import java.util.Map;

/**
 * Data provider for the survival demo. The same class is run by the Gradle
 * {@code foabicData} task and by the Foabic loader.
 */
public final class SurvivalData implements FoabicDataProvider {

    private static final ResourceLocation MINEABLE_AXE =
            ResourceLocation.parse("minecraft:mineable/axe");

    @Override
    public void generate(FoabicDataContext data) {
        data.blockStates(SurvivalMod.COLLECTOR)
                .simple("foabic_survival:block/feather_collector");
        data.itemModel(SurvivalMod.BLADE, "minecraft:item/handheld",
                Map.of("layer0", "foabic_survival:item/feather_blade"));

        data.recipe(SurvivalMod.COLLECTOR)
                .shaped("F F", "FCF", "FFF")
                .define('F', "minecraft:feather")
                .define('C', "minecraft:chest")
                .result(SurvivalMod.COLLECTOR.toString())
                .group("feather")
                .save();

        data.recipe(SurvivalMod.BLADE)
                .shaped(" F ", " F ", " S ")
                .define('F', "minecraft:feather")
                .define('S', "minecraft:stick")
                .result(SurvivalMod.BLADE.toString())
                .group("feather")
                .save();

        data.tag(TagType.ITEM, SurvivalMod.SWORDS).add(SurvivalMod.BLADE);
        data.tag(TagType.BLOCK, MINEABLE_AXE).add(SurvivalMod.COLLECTOR);
        data.lootTable(SurvivalMod.COLLECTOR).dropsSelf();

        data.lang("en_us")
                .put("block.foabic_survival.feather_collector", "Feather Collector")
                .put("item.foabic_survival.feather_blade", "Feather Blade")
                .put("entity.foabic_survival.feather_wisp", "Feather Wisp")
                .put("itemGroup.foabic_survival", "Foabic Survival");
        data.lang("zh_cn")
                .put("block.foabic_survival.feather_collector", "羽毛收集器")
                .put("item.foabic_survival.feather_blade", "羽毛剑")
                .put("entity.foabic_survival.feather_wisp", "羽毛精灵")
                .put("itemGroup.foabic_survival", "Foabic 生存");
    }
}
