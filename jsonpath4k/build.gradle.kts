import at.asitplus.gradle.Logger
import at.asitplus.gradle.coroutines
import at.asitplus.gradle.exportXCFramework
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization
import at.asitplus.gradle.setupDokka
import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.kmp.library)
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    alias(libs.plugins.antlr.kotlin.plugin)
    alias(libs.plugins.testballoon)
    id("signing")
    alias(libs.plugins.asp.conventions)
}

group = rootProject.group
version = rootProject.version

val SRCDIR_ANTRL = "generated/antlr"
val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    // compiling any *.g4 files within the project
    source = fileTree(layout.projectDirectory.dir("src")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    packageName = "at.asitplus.jsonpath.generated"

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are output inside src/gen/kotlin/{package-name}
    val outDir = "$SRCDIR_ANTRL/${packageName!!.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}

val Project.disableNdkTargets
    get() = ("true" == (System.getenv("disableNdkTargets")
        ?.also { Logger.lifecycle("  > Property disableNdkTargets set to $it from environment") }
        ?: runCatching {
            (project.extraProperties["disableNdkTargets"] as String).also {
                Logger.lifecycle("  > Property disableNdkTargets set to $it from extra properties")
            }
        }.getOrNull()))


kotlin {
    androidLibrary {
        namespace = "at.asitplus.jsonpath4k"
    }
    jvm()
    macosArm64()
    tvosArm64()
    tvosSimulatorArm64()
    iosArm64()
    iosSimulatorArm64()
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()

    if (project.hasAndroidSdk()) {
        if (project.hasAndroidNdk() && !project.disableNdkTargets) {
            androidNativeX64()
            androidNativeX86()
            androidNativeArm32()
            androidNativeArm64()
        } else {
            Logger.lifecycle("  > Skipping Android native targets (NDK missing or disableNdkTargets=true)")
        }
    }

    listOf(
        js().apply { browser { testTask { enabled = false } } },
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs().apply { browser { testTask { enabled = false } } },
        // wasmWasi()
    ).forEach {
        it.nodejs()
    }

    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir(SRCDIR_ANTRL))
            dependencies {
                implementation(libs.antlr.kotlin)
                implementation(serialization("json"))
                implementation(napier())
            }
        }
        commonTest {
            dependencies {
                // TestBalloon, the matrix addon, Kotest assertions and Kotest property are wired in
                // automatically by the asp-conventions plugin (unless TESTBALLOON_NO_ASP_HELPER is set).
                implementation(serialization("json"))
                // Needed by the highly-concurrent native ANTLR stress test in nativeTest.
                implementation(coroutines())
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateKotlinGrammarSource)
}
tasks.withType<org.gradle.jvm.tasks.Jar> {
    dependsOn(generateKotlinGrammarSource)
}
// Note: the JVM test task (JUnit Platform + launcher) is configured by the TestBalloon Gradle plugin,
// which the asp-conventions plugin applies; do not call useJUnitPlatform() manually.

exportXCFramework("JsonPath4K", transitiveExports = false)

val javadocJar = setupDokka(
    baseUrl = "https://github.com/a-sit-plus/jsonpath4k/tree/main"
)

val javadocRedirectJar = tasks.register<Jar>("javadocRedirectJar") {
    archiveClassifier.set("javadoc")
    from(project.rootDir.absolutePath+"/javadoc")
}

publishing {
    publications {
        withType<MavenPublication> {
            if (this.name != "relocation") artifact(javadocRedirectJar)
            pom {
                name.set("JsonPath4K")
                description.set("Kotlin Multiplatform library for using Json Paths as specified in [RFC9535](https://datatracker.ietf.org/doc/rfc9535/)")
                url.set("https://github.com/a-sit-plus/jsonpath4k")
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
                    connection.set("scm:git:git@github.com:a-sit-plus/jsonpath4k.git")
                    developerConnection.set("scm:git:git@github.com:a-sit-plus/jsonpath4k.git")
                    url.set("https://github.com/a-sit-plus/jsonpath4k")
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




fun Project.hasAndroidSdk() = resolveAndroidSdk(this)?.let { it -> isValidAndroidSdk(it) } == true

fun Project.hasAndroidNdk() = resolveAndroidNdk(this)?.let { it -> isValidAndroidNdk(it) } == true

private fun resolveAndroidSdk(project: Project): File? {
    // Highest precedence: ANDROID_SDK_ROOT (preferred), then ANDROID_HOME (legacy)
    val env = System.getenv()
    val fromEnv = listOf("ANDROID_SDK_ROOT", "ANDROID_HOME")
        .asSequence()
        .mapNotNull { env[it]?.takeIf { it.isNotBlank() } }
        .map(::File)
        .firstOrNull { it.exists() }

    if (fromEnv != null) return fromEnv

    // Fallback: local.properties (common on dev machines)
    val localProps = File(project.rootDir, "local.properties")
    if (localProps.exists()) {
        Properties().apply {
            localProps.inputStream().use(::load)
            (getProperty("sdk.dir") ?: getProperty("android.sdk.path"))?.let {
                val f = File(it)
                if (f.exists()) return f
            }
        }
    }
    return null
}

private fun resolveAndroidNdk(project: Project): File? {
    val env = System.getenv()
    val fromEnv = listOf("ANDROID_NDK_ROOT", "ANDROID_NDK_HOME", "ANDROID_NDK", "NDK_HOME")
        .asSequence()
        .mapNotNull { env[it]?.takeIf { it.isNotBlank() } }
        .map(::File)
        .firstOrNull { it.exists() }
    if (fromEnv != null) return fromEnv

    val localProps = File(project.rootDir, "local.properties")
    if (localProps.exists()) {
        Properties().apply {
            localProps.inputStream().use(::load)
            (getProperty("ndk.dir") ?: getProperty("ndkDirectory"))?.let {
                val f = File(it)
                if (f.exists()) return f
            }
        }
    }

    val sdk = resolveAndroidSdk(project) ?: return null
    val bundled = listOf(File(sdk, "ndk-bundle"), File(sdk, "ndk"))
        .firstOrNull { it.exists() }

    // $SDK/ndk is a folder containing versioned subfolders; pick the "latest" directory.
    if (bundled?.name == "ndk") {
        val candidates = bundled.listFiles()?.filter { it.isDirectory } ?: emptyList()
        return candidates.maxWithOrNull { a, b ->
            val ta = a.name.toVersionTuple()
            val tb = b.name.toVersionTuple()
            val n = maxOf(ta.size, tb.size)
            (0 until n).asSequence()
                .map { i -> (ta.getOrNull(i) ?: 0).compareTo(tb.getOrNull(i) ?: 0) }
                .firstOrNull { it != 0 } ?: 0
        } ?: bundled
    }

    return bundled
}

private fun String.toVersionTuple(): List<Int> =
    split('.', '-', '_').mapNotNull { it.toIntOrNull() }.ifEmpty { listOf(0) }

private fun isValidAndroidNdk(ndk: File): Boolean {
    val prebuilt = File(ndk, "toolchains/llvm/prebuilt")
    return prebuilt.isDirectory && (prebuilt.listFiles()?.any { it.isDirectory } == true)
}


private fun isValidAndroidSdk(sdk: File): Boolean {
    val platformsOk = File(sdk, "platforms").listFiles()?.any { it.isDirectory } == true
    val buildToolsOk = File(sdk, "build-tools").listFiles()?.any { it.isDirectory } == true
    return platformsOk && buildToolsOk
}
