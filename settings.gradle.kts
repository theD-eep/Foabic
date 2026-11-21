pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
    }
}

rootProject.name = "foabic"

include("api", "loader", "fabric", "neoforge", "example")
