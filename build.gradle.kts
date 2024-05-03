plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.gradle.nexus.publish)
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    mavenCentral()
    gradlePluginPortal()
}
nexusPublishing {
    repositories {
        sonatype()
    }
}

val artifactVersion: String by extra
group = "at.asitplus"
version = artifactVersion