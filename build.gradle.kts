plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    group = "com.okdori.stubbuilder"
    version = "0.0.9"
}
