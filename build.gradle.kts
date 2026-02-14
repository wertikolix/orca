plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    group = "ru.wertik"
    version = providers.gradleProperty("orcaVersion").orElse("0.2.0").get()
}
