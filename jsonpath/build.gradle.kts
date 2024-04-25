
import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.antlr.kotlin.plugin)
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain {
            kotlin {
                srcDir(layout.buildDirectory.dir("generatedAntlr"))
            }
            dependencies {
                implementation(libs.antlr.kotlin)
                implementation(libs.kotlinx.serialization)
                implementation(libs.napier)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotest.common)
                implementation(libs.kotest.property)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotest.framework.datatest)
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotlinx.serialization)
            }
        }
    }
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    // ANTLR .g4 files are under {example-project}/antlr
    // Only include *.g4 files. This allows tools (e.g., IDE plugins)
    // to generate temporary files inside the base path
    source = fileTree(layout.projectDirectory) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "at.asitplus.parser.generated"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}

tasks.withType<KotlinCompile<*>> {
    dependsOn(generateKotlinGrammarSource)
}
tasks.withType<AntlrKotlinTask> {
    dependsOn(tasks.named("jvmProcessResources"))
    dependsOn(tasks.named("iosX64ProcessResources"))
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}