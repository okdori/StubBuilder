plugins {
    kotlin("jvm") version "2.2.0" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.okdori.stubbuilder"
    version = "0.0.2"
}
