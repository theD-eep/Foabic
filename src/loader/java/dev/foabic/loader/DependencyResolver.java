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

/**
 * Resolves declared dependencies before any mod class is loaded.
 *
 * <p>Foabic mods, the Foabic API and the running Minecraft version are
 * resolved intrinsically. Platform-provided modules (Fabric API, Fabric
 * Loader, NeoForge) are supplied by the adapter. A missing or mismatched
 * requirement fails here with a {@link DependencyException} that names the mod,
 * the dependency, the requested range and the version that is actually
 * present.
 */
final class DependencyResolver {

    private final Map<String, DiscoveredMod> byId = new LinkedHashMap<>();
    private final Map<String, Version> provided = new HashMap<>();
    private final Map<String, Visit> states = new HashMap<>();
    private final Deque<String> path = new ArrayDeque<>();
    private final List<DiscoveredMod> ordered = new ArrayList<>();
    private final Version apiVersion = Version.parse(Foabic.VERSION);
    private final String platform;
    private final System.Logger log;

    DependencyResolver(List<DiscoveredMod> mods, Map<String, String> providedVersions,
                       String platform, System.Logger log) {
        this.platform = platform;
        this.log = log;
        for (DiscoveredMod mod : mods) byId.put(mod.metadata().id(), mod);

        provided.put("foabic-api", apiVersion);
        provided.put("foabic", apiVersion);
        provided.put("minecraft", Version.parse(Foabic.MINECRAFT));
        if (providedVersions != null) {
            for (Map.Entry<String, String> entry : providedVersions.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                try {
                    provided.put(entry.getKey(), Version.parse(entry.getValue()));
                } catch (IllegalArgumentException e) {
                    log.log(System.Logger.Level.WARNING,
                            "ignoring unparseable version of provided module {0}: {1}",
                            entry.getKey(), entry.getValue());
                }
            }
        }
    }

    List<DiscoveredMod> resolve() {
        for (DiscoveredMod mod : byId.values()) {
            VersionRange range = mod.metadata().apiRange();
            if (!range.test(apiVersion)) {
                throw new DependencyException("mod '" + mod.metadata().id()
                        + "' requires Foabic API " + range + ", but this runtime provides "
                        + apiVersion + ". Update the mod or install a matching Foabic build.");
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
        if (state == Visit.VISITING) throw new DependencyException("dependency cycle: " + cycle(id));

        states.put(id, Visit.VISITING);
        path.addLast(id);
        DiscoveredMod mod = byId.get(id);
        List<Dependency> required = new ArrayList<>(mod.metadata().dependencies());
        required.sort(Comparator.comparing(Dependency::id));
        for (Dependency dependency : required) {
            resolveRequired(id, dependency);
            if (byId.containsKey(dependency.id())) {
                visit(dependency.id());
            }
        }
        List<Dependency> optional = new ArrayList<>(mod.metadata().optionalDependencies());
        optional.sort(Comparator.comparing(Dependency::id));
        for (Dependency dependency : optional) {
            if (dependency.id().equals(id)) continue;
            DiscoveredMod target = byId.get(dependency.id());
            Version providedVersion = provided.get(dependency.id());
            if (target == null && providedVersion == null) continue;
            Version actual = target == null ? providedVersion : target.metadata().version();
            if (!dependency.matches(actual)) {
                log.log(System.Logger.Level.WARNING,
                        "ignoring optional dependency {0}@{1} of {2}: found {3}",
                        dependency.id(), dependency.range(), id, actual);
                continue;
            }
            if (target != null) visit(dependency.id());
        }
        path.removeLast();
        states.put(id, Visit.DONE);
        ordered.add(mod);
    }

    private void resolveRequired(String modId, Dependency dependency) {
        DiscoveredMod target = byId.get(dependency.id());
        if (target != null) {
            if (!dependency.matches(target.metadata().version())) {
                throw new DependencyException("mod '" + modId + "' requires '"
                        + dependency.id() + "' " + dependency.range() + ", but Foabic found "
                        + target.metadata().version() + " in " + target.jar().getFileName()
                        + ". Install a matching version or update the mod.");
            }
            return;
        }

        Version providedVersion = provided.get(dependency.id());
        if (providedVersion != null) {
            if (!dependency.matches(providedVersion)) {
                throw new DependencyException("mod '" + modId + "' requires '"
                        + dependency.id() + "' " + dependency.range() + ", but "
                        + displayName(dependency.id()) + " provides " + providedVersion
                        + ". Install a matching version.");
            }
            return;
        }

        throw new DependencyException(missingMessage(modId, dependency));
    }

    private String missingMessage(String modId, Dependency dependency) {
        String id = dependency.id();
        if (id.equals("fabric-api")) {
            if (platform.equals("neoforge")) {
                return "mod '" + modId + "' requires Fabric API " + dependency.range()
                        + ", but Fabric API is a Fabric-only mod and cannot load on NeoForge. "
                        + "Use a Fabric build of this mod or a NeoForge build of it.";
            }
            return "mod '" + modId + "' requires Fabric API " + dependency.range()
                    + ", but Fabric API is not installed. Put the matching Fabric API jar in "
                    + "mods/ (Minecraft 26.3 needs Fabric API 0.166.2+26.3).";
        }
        if (id.equals("fabricloader")) {
            return "mod '" + modId + "' requires Fabric Loader " + dependency.range()
                    + ", but this is the " + platform + " adapter. Use the Fabric build.";
        }
        if (id.equals("neoforge")) {
            return "mod '" + modId + "' requires NeoForge " + dependency.range()
                    + ", but this is the " + platform + " adapter. Use the NeoForge build.";
        }
        return "mod '" + modId + "' requires missing mod '" + id + "' " + dependency.range()
                + ". Put " + id + " in foabic-mods/ (or the platform mods/ directory if it is a "
                + "native mod) and restart, or remove the dependency from foabic.mod.";
    }

    private String displayName(String id) {
        return switch (id) {
            case "foabic-api", "foabic" -> "Foabic";
            case "minecraft" -> "Minecraft";
            case "fabric-api" -> "Fabric API";
            case "fabricloader" -> "Fabric Loader";
            case "neoforge" -> "NeoForge";
            default -> "'" + id + "'";
        };
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
