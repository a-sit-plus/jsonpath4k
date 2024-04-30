plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    mavenCentral()
    gradlePluginPortal()
}

val artifactVersion: String by extra
group = "at.asitplus"
version = artifactVersion
