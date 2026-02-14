import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

dependencies {
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit4)
}

tasks.test {
    useJUnit()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifactId = "fhmd-core"

            pom {
                name.set("FhMd Core")
                description.set("Core AST and parser mapping for FhMd")
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
