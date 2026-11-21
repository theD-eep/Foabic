plugins {
    `java-library`
}

val minecraftJar = rootProject.file(".cache/mc-26.3/server-nested.jar")
val minecraftLibraries = rootProject.fileTree(".cache/mc-bundle-libs/META-INF/libraries") {
    include("**/*.jar")
}

dependencies {
    api(project(":api"))
    compileOnly(files(minecraftJar))
    compileOnly(minecraftLibraries)
}

base {
    archivesName.set("foabic-loader")
}

sourceSets {
    main {
        java.setSrcDirs(listOf(rootProject.file("src/loader/java")))
    }
}
