pluginManagement {
    includeBuild("foabic-gradle-plugin")
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
    }
}

rootProject.name = "foabic"

include("api", "content", "loader", "fabric", "neoforge", "examples:minimal", "examples:chat", "examples:plugin-demo", "examples:survival")
