import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
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
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

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
                developers {
                    developer {
                        id.set("wertikolix")
                        name.set("Wertik")
                        email.set("wertikolix@users.noreply.github.com")
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
