
import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkConfig

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.kotlinx.serialization)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.antlr.kotlin.plugin)
    alias(libs.plugins.jetbrains.dokka)
    id("maven-publish")
    id("signing")
}

/* required for maven publication */
val artifactVersion: String by extra
group = "at.asitplus.jsonpath"
version = artifactVersion

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
                implementation(libs.jetbrains.kotlinx.serialization)
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
                implementation(libs.jetbrains.kotlinx.serialization)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
    }
}

exportIosFramework("JsonPath")

val javadocJar = setupDokka(
    baseUrl = "https://github.com/a-sit-plus/jsonpath/tree/main/",
    multiModuleDoc = false
)

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("JsonPath")
                description.set("Kotlin Multiplatform library for using Json Paths as specified in [RFC9535](https://datatracker.ietf.org/doc/rfc9535/)")
                url.set("https://github.com/a-sit-plus/jsonpath")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("acrusage")
                        name.set("Stefan Kreiner")
                        email.set("stefan.kreiner@iaik.tugraz.at")
                    }
                    developer {
                        id.set("nodh")
                        name.set("Christian Kollmann")
                        email.set("christian.kollmann@a-sit.at")
                    }
                    developer {
                        id.set("JesusMcCloud")
                        name.set("Bernd Prünster")
                        email.set("bernd.pruenster@a-sit.at")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:a-sit-plus/jsonpath.git")
                    developerConnection.set("scm:git:git@github.com:a-sit-plus/jsonpath.git")
                    url.set("https://github.com/a-sit-plus/jsonpath")
                }
            }
        }
    }
    repositories {
        mavenLocal {
            signing.isRequired = false
        }
        maven {
            url = uri(layout.projectDirectory.dir("..").dir("repo"))
            name = "local"
            signing.isRequired = false
        }
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")
    dependsOn(tasks.withType<ProcessResources>())
    dependsOn(tasks.withType<MetadataDependencyTransformationTask>())
    dependsOn(tasks.named<Task>("buildKotlinToolingMetadata"))

    // compiling any *.g4 files within the project
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

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
tasks.named<Task>("jvmSourcesJar") {
    dependsOn(generateKotlinGrammarSource)
}
tasks.named<Task>("dokkaHtml") {
    dependsOn(generateKotlinGrammarSource)
}
tasks.named<Task>("sourcesJar") {
    dependsOn(generateKotlinGrammarSource)
}



/**
 * taken from vclib conventions plugin at https://github.com/a-sit-plus/gradle-conventions-plugin
 */
fun Project.exportIosFramework(
    name: String,
    vararg additionalExports: Any
) = exportIosFramework(name, bitcodeEmbeddingMode = BitcodeEmbeddingMode.BITCODE, additionalExports = additionalExports)

fun Project.exportIosFramework(
    name: String,
    bitcodeEmbeddingMode: BitcodeEmbeddingMode,
    vararg additionalExports: Any
) {
    val iosTargets = kotlinExtension.let {
        if (it is KotlinMultiplatformExtension) {
            it.targets.filterIsInstance<KotlinNativeTarget>().filter { it.name.startsWith("ios") }
        } else throw StopExecutionException("No iOS Targets found! Declare them explicitly before calling exportIosFramework!")
    }

    extensions.getByType<KotlinMultiplatformExtension>().apply {
        XCFrameworkConfig(project, name).also { xcf ->
            logger.lifecycle("  \u001B[1mXCFrameworks will be exported for the following iOS targets: ${iosTargets.joinToString { it.name }}\u001B[0m")
            iosTargets.forEach {
                it.binaries.framework {
                    baseName = name
                    embedBitcode(bitcodeEmbeddingMode)
                    additionalExports.forEach { export(it) }
                    xcf.add(this)
                }
            }
        }
    }
}
fun Project.setupDokka(
    outputDir: String = layout.buildDirectory.dir("dokka").get().asFile.canonicalPath,
    baseUrl: String,
    multiModuleDoc: Boolean = false,
    remoteLineSuffix: String = "#L"
): TaskProvider<Jar> {
    val dokkaHtml = (tasks["dokkaHtml"] as DokkaTask).apply { outputDirectory.set(file(outputDir)) }

    val deleteDokkaOutput = tasks.register<Delete>("deleteDokkaOutputDirectory") {
        delete(outputDir)
    }
    val sourceLinktToConfigure = if (multiModuleDoc) (tasks["dokkaHtmlPartial"] as DokkaTaskPartial) else dokkaHtml
    sourceLinktToConfigure.dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(file("src/$name/kotlin"))
            remoteUrl.set(uri("$baseUrl/${project.name}/src/$name/kotlin").toURL())
            this@sourceLink.remoteLineSuffix.set(remoteLineSuffix)
        }
    }

    return tasks.register<Jar>("javadocJar") {
        dependsOn(deleteDokkaOutput, dokkaHtml)
        archiveClassifier.set("javadoc")
        from(outputDir)
    }
}