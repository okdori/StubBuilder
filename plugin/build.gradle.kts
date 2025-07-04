plugins {
    kotlin("jvm")
    `kotlin-dsl`
    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")

    implementation(project(":generator"))

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.1")
    testImplementation("io.mockk:mockk:1.14.2")
}

gradlePlugin {
    plugins {
        create("stubBuilderPlugin") {
            id = "io.github.okdori.stubbuilder"
            implementationClass = "com.okdori.stubbuilder.plugin.StubBuilderPlugin"
            displayName = "StubBuilder Gradle Plugin"
            description = """
                This Gradle plugin automatically generates MockK-based JUnit 5 test stubs for Spring @Service and @Transactional classes.
                It analyzes your service classes, identifies constructor dependencies for mocking, and creates a basic test structure,
                saving development time and promoting consistent test patterns.
                Supports automatic stub generation for create, update, delete, add, remove, and @DataMutator annotated methods.
            """.trimIndent()
            tags.set(listOf("spring", "test", "mockk", "stub", "code-generation", "kotlin"))
            website = "https://github.com/okdori/StubBuilder"
            vcsUrl = "https://github.com/okdori/StubBuilder.git"
        }
    }
}
