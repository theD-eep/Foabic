plugins {
    java
    id("dev.foabic.loom")
}

foabic {
    minecraftVersion = "26.3"
}

val minecraftJar = rootProject.file(".cache/mc-26.3/server-nested.jar")
val minecraftLibraries = rootProject.fileTree(".cache/mc-bundle-libs/META-INF/libraries") {
    include("**/*.jar")
}

dependencies {
    implementation(project(":content"))
    compileOnly(files(minecraftJar))
    compileOnly(minecraftLibraries)
}

base {
    archivesName.set("foabic-example-survival")
}
