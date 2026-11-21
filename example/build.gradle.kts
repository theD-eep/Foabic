plugins {
    java
}

val minecraftJar = rootProject.file(".cache/mc-26.3/server-nested.jar")
val minecraftLibraries = rootProject.fileTree(".cache/mc-bundle-libs/META-INF/libraries") {
    include("**/*.jar")
}

dependencies {
    implementation(project(":api"))
    compileOnly(files(minecraftJar))
    compileOnly(minecraftLibraries)
}

base {
    archivesName.set("foabic-example-minimal")
}

sourceSets {
    main {
        java.setSrcDirs(listOf(rootProject.file("examples/minimal/src")))
        resources.setSrcDirs(listOf(rootProject.file("examples/minimal/resources")))
    }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE"))
}
