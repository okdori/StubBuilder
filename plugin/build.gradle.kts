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
            id = "io.github.okdori.stubbuilder"
            // FQCN(Fully Qualified Class Name) 지정
            implementationClass = "com.okdori.stubbuilder.plugin.GenerateTestStubTask"

            displayName = "StubBuilder Gradle Plugin"
            description = """
                This Gradle plugin automatically generates MockK-based JUnit 5 test stubs for Spring @Service and @Transactional classes.
                It analyzes your service classes, identifies constructor dependencies for mocking, and creates a basic test structure,
                saving development time and promoting consistent test patterns.
                Supports automatic stub generation for create, update, delete, add, remove, and @DataMutator annotated methods.
            """.trimIndent()
            tags.set(listOf("stub", "test", "generation", "kotlin", "automation"))
            website = "https://github.com/okdori/StubBuilder"
            vcsUrl = "https://github.com/okdori/StubBuilder.git"
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

signing {
    useGpgCmd()
    sign(publishing.publications.getByName("mavenJava"))
}
