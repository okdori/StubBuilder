plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("com.squareup:kotlinpoet:2.2.0")
    compileOnly("org.springframework:spring-context:6.2.7") // @Service 어노테이션
    compileOnly("org.springframework:spring-tx:6.2.7") // @Transactional 어노테이션
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("io.mockk:mockk:1.14.2")
    implementation("org.junit.jupiter:junit-jupiter-api:5.13.1")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.1")
    testImplementation("org.assertj:assertj-core:3.26.0")

    compileOnly(project(":common"))
}

tasks.test {
    useJUnitPlatform()
}
