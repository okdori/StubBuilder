plugins {
    kotlin("jvm") version "1.9.23" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.okdori.stubbuilder"
    version = "1.0.0"
}
