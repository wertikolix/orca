plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":orca-core"))
}

application {
    mainClass.set("ru.wertik.orca.benchmarks.MainKt")
    // A fixed heap keeps timings comparable between machines and CI runners.
    applicationDefaultJvmArgs = listOf("-Xms512m", "-Xmx1g", "-XX:+AlwaysPreTouch")
}
