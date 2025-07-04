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
            id = "com.okdori.stubbuilder.new"
            implementationClass = "com.okdori.stubbuilder.plugin.StubBuilderPlugin"
            displayName = "StubBuilder Gradle Plugin"
            description = "Spring @Service 및 @Transactional 클래스에 대한 MockK 기반 테스트 스텁을 자동으로 생성"
            tags.set(listOf("spring", "test", "mockk", "stub", "code-generation", "kotlin"))
            website = "https://github.com/okdori/StudBuilder"
            vcsUrl = "https://github.com/okdori/StudBuilder.git"
        }
    }
}
