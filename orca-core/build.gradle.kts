import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(17)

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.intellij.markdown)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.junit4)
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "kotlinMultiplatform") {
            "orca-core"
        } else {
            "orca-core-$name"
        }

        pom {
            name.set("Orca Core")
            description.set("Core AST and parser mapping for Orca")
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

signing {
    useGpgCmd()
    sign(publishing.publications)
}
