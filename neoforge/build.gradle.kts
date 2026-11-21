plugins {
    java
}

val minecraftJar = rootProject.file(".cache/mc-26.3/server-nested.jar")
val minecraftLibraries = rootProject.fileTree(".cache/mc-bundle-libs/META-INF/libraries") {
    include("**/*.jar")
}
val neoForgeJar = rootProject.file(
    ".cache/neoforge-server/libraries/net/neoforged/neoforge/26.3.0.115/neoforge-26.3.0.115-universal.jar"
)
val neoForgeLibraries = rootProject.fileTree(".cache/neoforge-server/libraries") {
    include("**/*.jar")
}

dependencies {
    implementation(project(":loader"))
    compileOnly(files(minecraftJar))
    compileOnly(minecraftLibraries)
    compileOnly(files(neoForgeJar))
    compileOnly(neoForgeLibraries)
}

base {
    archivesName.set("foabic-neoforge")
}

sourceSets {
    main {
        java.setSrcDirs(listOf(rootProject.file("src/neoforge/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/neoforge/resources")))
    }
}

tasks.named<Jar>("jar") {
    dependsOn(":api:classes", ":content:classes", ":loader:classes")
    from(rootProject.project(":api").layout.buildDirectory.dir("classes/java/main"))
    from(rootProject.project(":api").layout.buildDirectory.dir("resources/main"))
    from(rootProject.project(":content").layout.buildDirectory.dir("classes/java/main"))
    from(rootProject.project(":content").layout.buildDirectory.dir("resources/main"))
    from(rootProject.project(":loader").layout.buildDirectory.dir("classes/java/main"))
    from(rootProject.project(":loader").layout.buildDirectory.dir("resources/main"))
    from(rootProject.file("LICENSE"))
}
