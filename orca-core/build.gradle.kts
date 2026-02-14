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
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.task.list.items)

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
            artifactId = "orca-core"
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

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
