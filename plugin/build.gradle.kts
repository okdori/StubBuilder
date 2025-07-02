plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
}

gradlePlugin {
    plugins {
        create("stubBuilderPlugin") {
            id = "com.okdori.stubbuilder"
            artifacts { id = "stubbuilder-plugin" }
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar.get().archiveFile.get()) {
                classifier = ""
            }
            groupId = project.group.toString()
            artifactId = "stubbuilder-plugin"
            version = project.version.toString()
        }
    }
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}
