plugins {
    `java-gradle-plugin`
}

group = "dev.foabic"
version = "1.3.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    plugins {
        create("foabicLoom") {
            id = "dev.foabic.loom"
            implementationClass = "dev.foabic.gradle.FoabicGradlePlugin"
        }
    }
}
