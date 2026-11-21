package dev.foabic.loader.mod;

import java.net.URL;
import java.net.URLClassLoader;

public final class ModClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    public ModClassLoader(URL[] urls, ClassLoader parent) {
        super("FoabicMods", urls, parent);
    }
}
