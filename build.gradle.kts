plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("com.gradle.plugin-publish") version "1.3.1" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    group = "com.okdori.stubbuilder"
    version = "0.0.1"
}
