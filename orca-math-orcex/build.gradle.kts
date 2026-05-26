import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
    signing
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":orca-compose"))
    implementation(libs.compose.ui)
    implementation(libs.orcex.render.android)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                artifactId = "orca-math-orcex"
                artifact(tasks.register("releaseJavadocJar", Jar::class) {
                    archiveBaseName.set("orca-math-orcex")
                    archiveVersion.set(project.version.toString())
                    archiveClassifier.set("javadoc")
                })
                pom {
                    name.set("Orca Math Orcex")
                    description.set("Optional native Android Orcex math renderer for Orca Compose")
                    url.set("https://github.com/wertikolix/Orca")
                    licenses { license { name.set("MIT License"); url.set("https://opensource.org/licenses/MIT") } }
                    developers { developer { id.set("wertikolix"); name.set("Wertik") } }
                    scm { url.set("https://github.com/wertikolix/Orca") }
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
}

signing {
    if (!providers.gradleProperty("skipSigning").map(String::toBoolean).orElse(false).get()) {
        val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
            ?: providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").orNull
            ?: providers.environmentVariable("SIGNING_IN_MEMORY_KEY").orNull
        val signingKeyPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull
            ?: providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword").orNull
            ?: providers.environmentVariable("SIGNING_IN_MEMORY_KEY_PASSWORD").orNull
        if (signingKey.isNullOrBlank()) useGpgCmd() else useInMemoryPgpKeys(signingKey, signingKeyPassword ?: "")
        sign(publishing.publications)
    }
}
