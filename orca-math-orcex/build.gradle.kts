import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
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
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(project(":orca-compose"))
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.orcex.render.compose)
        }
    }
}

android {
    namespace = "ru.wertik.orca.math.orcex"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
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
        val publicationArtifactId = "orca-math-orcex$platformSuffix"
        artifactId = publicationArtifactId

        val javadocJar = tasks.register("${name}JavadocJar", Jar::class) {
            archiveBaseName.set(publicationArtifactId)
            archiveVersion.set(project.version.toString())
            archiveClassifier.set("javadoc")
        }
        artifact(javadocJar)

        pom {
            name.set("Orca Math Orcex")
            description.set("Optional Compose Multiplatform Orcex math renderer for Orca")
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
            name = "centralStaging"
            url = uri(providers.gradleProperty("centralStagingRepoUrl").orElse("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/").get())
            credentials {
                username = providers.gradleProperty("centralTokenUsername").orNull
                password = providers.gradleProperty("centralTokenPassword").orNull
            }
        }
    }
}
