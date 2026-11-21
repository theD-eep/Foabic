
plugins {
    id("net.fabricmc.fabric-loom") version "1.14.10"
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://libraries.minecraft.net/") { name = "Mojang" }
}

dependencies {
    minecraft("com.mojang:minecraft:26.3")

    implementation(project(":loader"))
    implementation("net.fabricmc:fabric-loader:0.20.1")
    implementation("net.fabricmc.fabric-api:fabric-api:0.166.2+26.3")
}

base {
    archivesName.set("foabic-fabric")
}

sourceSets {
    main {
        java.setSrcDirs(listOf(rootProject.file("src/fabric/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/fabric/resources")))
    }
}

tasks.named<Jar>("jar") {
    dependsOn(":api:classes", ":loader:classes")
    from(rootProject.project(":api").layout.buildDirectory.dir("classes/java/main"))
    from(rootProject.project(":api").layout.buildDirectory.dir("resources/main"))
    from(rootProject.project(":loader").layout.buildDirectory.dir("classes/java/main"))
    from(rootProject.project(":loader").layout.buildDirectory.dir("resources/main"))
    from(rootProject.file("LICENSE"))
}
