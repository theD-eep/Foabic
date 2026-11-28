import java.security.MessageDigest
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    base
    java
}

allprojects {
    group = "dev.foabic"
    version = "1.3.0"
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://libraries.minecraft.net/") { name = "Mojang" }
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(25)
            options.encoding = "UTF-8"
        }
    }
}

val minecraftJar = file(".cache/mc-26.3/server-nested.jar")
val minecraftLibraries = fileTree(".cache/mc-bundle-libs/META-INF/libraries") {
    include("**/*.jar")
}

sourceSets {
    test {
        java.setSrcDirs(listOf(file("src/test/java")))
    }
}

dependencies {
    testImplementation(project(":api"))
    testImplementation(project(":content"))
    testImplementation(project(":loader"))
    testImplementation(files(minecraftJar))
    testImplementation(minecraftLibraries)
}

val coreTest = tasks.register<JavaExec>("coreTest") {
    group = "verification"
    description = "Runs the platform-neutral Foabic core tests."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.foabic.test.AllTests")
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}

tasks.named("check") {
    dependsOn(coreTest, ":api:check")
}

val apiJar = project(":api").layout.buildDirectory.file("libs/foabic-api-1.3.0.jar")
val contentJar = project(":content").layout.buildDirectory.file("libs/foabic-content-1.3.0.jar")
val fabricJar = project(":fabric").layout.buildDirectory.file("libs/foabic-fabric-1.3.0.jar")
val neoForgeJar = project(":neoforge").layout.buildDirectory.file("libs/foabic-neoforge-1.3.0.jar")
val exampleJar = project(":examples:minimal").layout.buildDirectory.file("libs/foabic-example-minimal-1.3.0.jar")
val exampleChatJar = project(":examples:chat").layout.buildDirectory.file("libs/foabic-example-chat-1.3.0.jar")
val survivalFabricJar = project(":examples:survival").layout.projectDirectory.file("dist/mod-fabric.jar")
val survivalNeoForgeJar = project(":examples:survival").layout.projectDirectory.file("dist/mod-neoforge.jar")

val dist by tasks.registering(Copy::class) {
    group = "build"
    description = "Copies the release jars and checksums into the root dist/ directory."
    dependsOn(":api:jar", ":content:jar", ":fabric:jar", ":neoforge:jar",
        ":examples:minimal:jar", ":examples:chat:jar", ":examples:survival:foabicDist")
    doFirst {
        delete(layout.projectDirectory.dir("dist"))
    }
    from(apiJar)
    from(contentJar)
    from(fabricJar)
    from(neoForgeJar)
    from(exampleJar)
    from(exampleChatJar)
    from(survivalFabricJar) { rename { "foabic-example-survival-fabric-1.3.0.jar" } }
    from(survivalNeoForgeJar) { rename { "foabic-example-survival-neoforge-1.3.0.jar" } }
    into(layout.projectDirectory.dir("dist"))
    doLast {
        val output = layout.projectDirectory.dir("dist").asFile
        val jars = output.listFiles { file -> file.isFile && file.extension == "jar" }
            ?.sortedBy { it.name }
            .orEmpty()
        val digest = MessageDigest.getInstance("SHA-256")
        val lines = jars.map { file ->
            digest.reset()
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
            "$hash  ${file.name}"
        }
        output.resolve("SHA256SUMS").writeText(lines.joinToString("\n") + "\n")
    }
}

tasks.named("build") {
    dependsOn(dist)
}

tasks.named("clean") {
    doLast {
        delete(layout.projectDirectory.dir("dist"))
    }
}
