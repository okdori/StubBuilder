plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(gradleApi())
    implementation(project(":generator"))
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

tasks {
    shadowJar {
        archiveClassifier.set("all")
        isZip64 = true
    }

    named("assemble") {
        dependsOn(shadowJar)
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

tasks.named("publishMavenJavaPublicationToMavenLocal") {
    dependsOn(tasks.shadowJar)
}
