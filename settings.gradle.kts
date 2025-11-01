pluginManagement {
    repositories {
        google() // Add Google repository for plugins
        mavenCentral() // Add Maven Central repository for plugins
    }
}

dependencyResolutionManagement {
    repositories {
        google() // Add Google repository for dependencies
        mavenCentral() // Add Maven Central repository for dependencies
    }
}

rootProject.name = "RideDrop"

include(":app") // Include the app module
