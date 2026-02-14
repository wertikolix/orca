import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "ru.wertik.fhmd.compose.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    api(project(":fhmd-core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "fhmd-compose-android"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("FhMd Compose Android")
                description.set("Android Compose renderer for FhMd")
                url.set("https://github.com/wertikolix/FhMd")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/wertikolix/FhMd.git")
                    developerConnection.set("scm:git:ssh://git@github.com/wertikolix/FhMd.git")
                    url.set("https://github.com/wertikolix/FhMd")
                }
            }
        }
    }

    repositories {
        maven {
            name = "github"
            url = uri(
                providers.gradleProperty("fhmdMavenRepoUrl")
                    .orElse("https://maven.pkg.github.com/wertikolix/FhMd")
                    .get(),
            )
            credentials {
                username = providers.gradleProperty("fhmdMavenUsername").orNull
                password = providers.gradleProperty("fhmdMavenPassword").orNull
            }
        }
    }
}
