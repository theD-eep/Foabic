package dev.foabic.loader;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Dependency;
import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DependencyResolver {

    private final Map<String, DiscoveredMod> byId = new LinkedHashMap<>();
    private final Map<String, Visit> states = new HashMap<>();
    private final Deque<String> path = new ArrayDeque<>();
    private final List<DiscoveredMod> ordered = new ArrayList<>();
    private final Version apiVersion = Version.parse(Foabic.VERSION);
    private final System.Logger log;

    DependencyResolver(List<DiscoveredMod> mods, System.Logger log) {
        this.log = log;
        for (DiscoveredMod mod : mods) byId.put(mod.metadata().id(), mod);
    }

    List<DiscoveredMod> resolve() {
        for (DiscoveredMod mod : byId.values()) {
            VersionRange range = mod.metadata().apiRange();
            if (!range.test(apiVersion)) {
                throw new ModLoadException("mod '" + mod.metadata().id()
                        + "' requires Foabic API " + range + ", loader is " + apiVersion);
            }
        }
        for (String id : byId.values().stream().map(m -> m.metadata().id()).sorted().toList()) {
            visit(id);
        }
        return List.copyOf(ordered);
    }

    private void visit(String id) {
        Visit state = states.get(id);
        if (state == Visit.DONE) return;
        if (state == Visit.VISITING) throw new ModLoadException("dependency cycle: " + cycle(id));

        states.put(id, Visit.VISITING);
        path.addLast(id);
        DiscoveredMod mod = byId.get(id);
        List<Dependency> required = new ArrayList<>(mod.metadata().dependencies());
        required.sort(Comparator.comparing(Dependency::id));
        for (Dependency dependency : required) {
            DiscoveredMod target = byId.get(dependency.id());
            if (target == null) {
                throw new ModLoadException("mod '" + id + "' requires missing dependency '"
                        + dependency.id() + "' (" + dependency.range() + ")");
            }
            if (!dependency.matches(target.metadata().version())) {
                throw new ModLoadException("mod '" + id + "' requires '" + dependency.id()
                        + "' " + dependency.range() + ", found " + target.metadata().version());
            }
            visit(dependency.id());
        }
        List<Dependency> optional = new ArrayList<>(mod.metadata().optionalDependencies());
        optional.sort(Comparator.comparing(Dependency::id));
        for (Dependency dependency : optional) {
            if (dependency.id().equals(id)) continue;
            DiscoveredMod target = byId.get(dependency.id());
            if (target == null) continue;
            if (!dependency.matches(target.metadata().version())) {
                log.log(System.Logger.Level.WARNING,
                        "ignoring optional dependency {0}@{1} of {2}: found {3}",
                        dependency.id(), dependency.range(), id, target.metadata().version());
                continue;
            }
            visit(dependency.id());
        }
        path.removeLast();
        states.put(id, Visit.DONE);
        ordered.add(mod);
    }

    private String cycle(String first) {
        StringBuilder builder = new StringBuilder();
        boolean emit = false;
        for (String id : path) {
            if (id.equals(first)) emit = true;
            if (emit) {
                if (builder.length() > 0) builder.append(" -> ");
                builder.append(id);
            }
        }
        return builder.append(" -> ").append(first).toString();
    }

    private enum Visit { VISITING, DONE }
}
