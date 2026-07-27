import org.gradle.kotlin.dsl.support.listFilesOrdered

plugins {
    kotlin("multiplatform") version libs.versions.kotlin.get() apply false
    kotlin("plugin.serialization") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.asp.conventions)
}

val artifactVersion: String by extra
group = "at.asitplus"
version = artifactVersion

tasks.register<Sync>("dokkaGenerateSite") {
    dependsOn(":jsonpath4k:dokkaGeneratePublicationHtml")
    from(project(":jsonpath4k").layout.buildDirectory.dir("dokka/html"))
    from(rootDir.listFilesOrdered { it.extension.lowercase() == "png" || it.extension.lowercase() == "svg" })
    into(rootProject.layout.projectDirectory.dir("docs"))
}