plugins {
    kotlin("multiplatform") version libs.versions.kotlin.get() apply false
    kotlin("plugin.serialization") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.asp.conventions)
}

val artifactVersion: String by extra
group = "at.asitplus"
version = artifactVersion
