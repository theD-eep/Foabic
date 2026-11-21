plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
}

base {
    archivesName.set("foabic-loader")
}

sourceSets {
    main {
        java.setSrcDirs(listOf(rootProject.file("src/loader/java")))
    }
}
