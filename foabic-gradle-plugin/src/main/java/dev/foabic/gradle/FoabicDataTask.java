package dev.foabic.gradle;

import groovy.json.JsonSlurper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs a mod's {@code data} providers before resources are packaged.
 *
 * <p>The providers only need the Foabic content API. Their output is a normal
 * {@code assets/} + {@code data/} tree, included in both platform jars by the
 * regular {@code processResources} pipeline.
 */
public abstract class FoabicDataTask extends DefaultTask {

    private File metadataFile;
    private FileCollection classpath;
    private File outputDirectory;

    @InputFile
    @Optional
    public File getMetadataFile() {
        return metadataFile;
    }

    public void setMetadataFile(File metadataFile) {
        this.metadataFile = metadataFile;
    }

    @Classpath
    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    @OutputDirectory
    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Inject
    public abstract ExecOperations getExecOperations();

    @TaskAction
    public void generate() {
        if (metadataFile == null || !metadataFile.isFile()) {
            getLogger().lifecycle("FOABIC_DATAGEN skipped: {} does not exist", metadataFile);
            return;
        }

        Map<String, Object> metadata = readMetadata();
        String id = string(metadata.get("id"));
        List<String> providers = providers(metadata);
        if (id == null || id.isBlank() || providers.isEmpty()) {
            getLogger().lifecycle("FOABIC_DATAGEN skipped: no data provider declared");
            return;
        }

        try {
            if (outputDirectory.exists()) {
                Files.walk(outputDirectory.toPath())
                        .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new GradleException("cannot clean " + path, e);
                            }
                        });
            }
            Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            throw new GradleException("cannot prepare Foabic data output " + outputDirectory, e);
        }

        List<String> args = new ArrayList<>();
        args.add("--id");
        args.add(id);
        args.add("--output");
        args.add(outputDirectory.getAbsolutePath());
        for (String provider : providers) {
            args.add("--provider");
            args.add(provider);
        }

        getExecOperations().javaexec(spec -> {
            spec.setClasspath(classpath);
            spec.getMainClass().set("dev.foabic.api.data.internal.FoabicDataMain");
            spec.setArgs(args);
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMetadata() {
        try {
            Object parsed = new JsonSlurper().parse(metadataFile, StandardCharsets.UTF_8.name());
            if (!(parsed instanceof Map<?, ?> map)) {
                throw new GradleException(metadataFile + " must contain a JSON object");
            }
            return (Map<String, Object>) map;
        } catch (RuntimeException e) {
            throw new GradleException("Cannot parse " + metadataFile + ": " + e.getMessage(), e);
        }
    }

    private static List<String> providers(Map<String, Object> metadata) {
        Object value = metadata.containsKey("data")
                ? metadata.get("data") : metadata.get("dataProviders");
        List<String> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                if (entry != null && !String.valueOf(entry).isBlank()) {
                    result.add(String.valueOf(entry).trim());
                }
            }
        } else if (value != null) {
            for (String part : String.valueOf(value).split(",")) {
                if (!part.isBlank()) result.add(part.trim());
            }
        }
        return result;
    }

    private static String string(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
