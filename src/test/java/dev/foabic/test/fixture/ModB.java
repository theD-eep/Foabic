package dev.foabic.test.fixture;

import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;

public final class ModB implements ModInitializer {

    @Override
    public void onInitialize(ModContext context) {
        TestMod.ORDER.add("b");
    }
}
