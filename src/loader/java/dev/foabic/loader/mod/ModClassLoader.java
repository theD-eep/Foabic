package dev.foabic.loader.mod;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * One shared, parent-first class loader for all mods. Sharing means mods can
 * call each other directly; API and game classes always win over a bundled
 * copy. Mod jars are never extracted.
 */
public final class ModClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    public ModClassLoader(URL[] urls, ClassLoader parent) {
        super("FoabicMods", urls, parent);
    }
}
