plugins {
    kotlin("jvm") version "2.0.0" apply false
    id("com.gradle.plugin-publish") version "1.2.1" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    group = "io.github.okdori.stubbuilder"
    version = "1.0.0"
}
