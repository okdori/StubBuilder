plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("com.gradle.plugin-publish")
    id("signing")
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

            displayName = "StubBuilder Gradle Plugin"
            tags.set(listOf("stub", "test", "generation", "kotlin", "automation"))
            website = "https://github.com/okdori/StudBuilder"
            vcsUrl = "https://github.com/okdori/StudBuilder.git"
        }
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        isZip64 = true
    }

    named("assemble") {
        dependsOn(shadowJar)
    }

    named("publishPlugins") {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar.get().archiveFile.get()) {
            }
            groupId = project.group.toString()
            artifactId = "stubbuilder-plugin"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
