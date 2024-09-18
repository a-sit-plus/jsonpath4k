rootProject.name = "JsonPath4K"

pluginManagement {
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots") //KOTEST snapshot
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

include(":jsonpath4k")