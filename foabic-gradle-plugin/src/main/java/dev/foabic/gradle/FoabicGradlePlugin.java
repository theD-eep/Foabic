package dev.foabic.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;

/**
 * Foabic Loom: package one source tree as a Fabric and a NeoForge Foabic mod.
 *
 * <pre>{@code
 * plugins { id("dev.foabic.loom") version "1.2.0" }
 * foabic { minecraftVersion = "26.3" }
 * }</pre>
 *
 * The plugin does not choose a Minecraft development environment. Add the
 * Foabic content API and your Minecraft compile classpath as usual; this
 * plugin owns dual-jar packaging and metadata translation.
 */
public final class FoabicGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java");
        FoabicExtension extension = project.getExtensions()
                .create("foabic", FoabicExtension.class);

        File buildDirectory = project.getLayout().getBuildDirectory().get().getAsFile();
        File metadataRoot = new File(buildDirectory, "foabic-metadata");
        File jarsDirectory = new File(buildDirectory, "foabic-jars");
        File resourcesDirectory = new File(project.getProjectDir(), "src/main/resources");
        File metadataFile = new File(resourcesDirectory, "foabic.mod.json");

        var metadata = project.getTasks().register("foabicMetadata", FoabicMetadataTask.class, task -> {
            task.setGroup("foabic");
            task.setDescription("Generates foabic.mod.json and both platform metadata files.");
            task.setMetadataFile(metadataFile);
            task.setResourcesDirectory(resourcesDirectory);
            task.setOutputRoot(metadataRoot);
            task.setExtension(extension);
            task.setProjectName(project.getName());
            task.setProjectVersion(project.getVersion().toString());
            task.getOutputs().upToDateWhen(ignored -> false);
        });

        var fabricJar = project.getTasks().register("foabicFabricJar", Jar.class, task -> {
            task.setGroup("foabic");
            task.setDescription("Builds dist/mod-fabric.jar.");
            task.dependsOn(metadata);
            configureArchive(project, task, "mod-fabric.jar", jarsDirectory);
            task.from(project.getLayout().getBuildDirectory().dir("foabic-metadata/common"));
            task.from(project.getLayout().getBuildDirectory().dir("foabic-metadata/fabric"));
        });

        var neoForgeJar = project.getTasks().register("foabicNeoForgeJar", Jar.class, task -> {
            task.setGroup("foabic");
            task.setDescription("Builds dist/mod-neoforge.jar.");
            task.dependsOn(metadata);
            configureArchive(project, task, "mod-neoforge.jar", jarsDirectory);
            task.from(project.getLayout().getBuildDirectory().dir("foabic-metadata/common"));
            task.from(project.getLayout().getBuildDirectory().dir("foabic-metadata/neoforge"));
        });

        project.getTasks().register("foabicDist", Sync.class, task -> {
            task.setGroup("build");
            task.setDescription("Puts mod-fabric.jar and mod-neoforge.jar into dist/.");
            task.dependsOn(fabricJar, neoForgeJar);
            task.from(jarsDirectory);
            task.into(project.getLayout().getProjectDirectory().dir("dist"));
        });

        project.getTasks().named("build").configure(task -> task.dependsOn("foabicDist"));
        project.getTasks().named("clean").configure(task -> task.doLast(ignored -> {
            project.delete(project.getLayout().getProjectDirectory().dir("dist"));
        }));
    }

    private static void configureArchive(Project project, Jar task, String name, File directory) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        task.getArchiveFileName().set(name);
        task.getDestinationDirectory().set(directory);
        task.from(sourceSets.getByName("main").getOutput(), spec ->
                spec.exclude("fabric.mod.json", "META-INF/neoforge.mods.toml", "foabic.mod.json"));
        task.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
        task.getManifest().getAttributes().put("Implementation-Title", "Foabic mod");
        task.getManifest().getAttributes().put("Implementation-Version",
                project.getVersion().toString());
    }
}
