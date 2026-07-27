pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Orca"

include(
    ":orca-benchmarks",
    ":orca-core",
    ":orca-compose",
    ":orca-compose-material3",
    ":orca-images-coil",
    ":orca-math-orcex",
    ":sample-app",
)
