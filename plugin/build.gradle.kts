plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.github.johnrengelman.shadow")
}

gradlePlugin {
    plugins {
        create("stubBuilderPlugin") {
            id = "com.okdori.stubbuilder"
            // FQCN(Fully Qualified Class Name) 지정
            implementationClass = "com.okdori.stubbuilder.plugin.GenerateTestStubTask"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(project(":generator"))
}

tasks {
    shadowJar {
        isZip64 = true
    }
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}
