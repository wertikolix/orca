import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        publishLibraryVariants("release")
    }

    jvm("desktop")

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(project(":orca-core"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            implementation(compose.animation)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "ru.wertik.orca.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

signing {
    if (!providers.gradleProperty("skipSigning").map(String::toBoolean).orElse(false).get()) {
        val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
            ?: providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").orNull
            ?: providers.environmentVariable("SIGNING_IN_MEMORY_KEY").orNull
        val signingKeyPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull
            ?: providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword").orNull
            ?: providers.environmentVariable("SIGNING_IN_MEMORY_KEY_PASSWORD").orNull

        if (signingKey.isNullOrBlank()) {
            useGpgCmd()
        } else {
            useInMemoryPgpKeys(signingKey, signingKeyPassword ?: "")
        }
        sign(publishing.publications)
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        val platformSuffix = when (name) {
            "kotlinMultiplatform" -> ""
            "androidRelease" -> "-android"
            else -> "-$name"
        }
        val publicationArtifactId = "orca-compose$platformSuffix"
        artifactId = publicationArtifactId

        val javadocJar = tasks.register("${name}JavadocJar", Jar::class) {
            archiveBaseName.set(publicationArtifactId)
            archiveVersion.set(project.version.toString())
            archiveClassifier.set("javadoc")
        }
        artifact(javadocJar)

        pom {
            name.set("Orca Compose")
            description.set("Compose Multiplatform renderer for Orca")
            url.set("https://github.com/wertikolix/Orca")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("wertikolix")
                    name.set("Wertik")
                    email.set("wertikolix@users.noreply.github.com")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/wertikolix/Orca.git")
                developerConnection.set("scm:git:ssh://git@github.com/wertikolix/Orca.git")
                url.set("https://github.com/wertikolix/Orca")
            }
        }
    }

    repositories {
        maven {
            name = "github"
            url = uri(
                providers.gradleProperty("orcaMavenRepoUrl")
                    .orElse("https://maven.pkg.github.com/wertikolix/Orca")
                    .get(),
            )
            credentials {
                username = providers.gradleProperty("orcaMavenUsername").orNull
                password = providers.gradleProperty("orcaMavenPassword").orNull
            }
        }
        maven {
            name = "centralStaging"
            url = uri(
                providers.gradleProperty("centralStagingRepoUrl")
                    .orElse("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                    .get(),
            )
            credentials {
                username = providers.gradleProperty("centralTokenUsername").orNull
                password = providers.gradleProperty("centralTokenPassword").orNull
            }
        }
    }
}
