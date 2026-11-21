plugins {
    `java-library`
}

base {
    archivesName.set("foabic-api")
}

sourceSets {
    main {
        java.setSrcDirs(listOf(rootProject.file("src/api/java")))
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.debugOptions.debugLevel = "none"
}

tasks.named<Jar>("jar") {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    entryCompression = ZipEntryCompression.DEFLATED
}

val verifyApiContract by tasks.registering {
    group = "verification"
    description = "Fails if the core API jar is 15 KiB or larger, or contains platform imports."
    dependsOn(tasks.named("jar"))
    doLast {
        val jar = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        val limit = 15 * 1024L
        if (jar.length() >= limit) {
            throw GradleException("foabic-api jar is ${jar.length()} bytes; limit is < $limit")
        }
        val forbidden = Regex("""net\.(fabricmc|neoforged)\.""")
        val offenders = mutableListOf<String>()
        rootProject.fileTree("src") {
            include("api/**/*.java", "content/**/*.java")
        }.forEach { file ->
            if (forbidden.containsMatchIn(file.readText())) {
                offenders += file.relativeTo(rootProject.projectDir).path
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException("Platform imports are forbidden in API sources: $offenders")
        }
        logger.lifecycle("FOABIC_API_SIZE bytes={} limit={}", jar.length(), limit)
    }
}

tasks.named("check") {
    dependsOn(verifyApiContract)
}
