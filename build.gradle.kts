// Root build.gradle.kts file (Project Level)

plugins {
    // Plugin definitions for Gradle versioning
    id("com.android.application") version "8.8.2" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
}

allprojects {
    repositories {
        google()        // Add Google repository
        mavenCentral()  // Add Maven Central repository
    }
}

buildscript {
    repositories {
        google()        // Add Google repository
        mavenCentral()  // Add Maven Central repository
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.2")  // Android Gradle Plugin
        classpath("com.google.gms:google-services:4.3.15")  // Firebase Plugin
    }
}
