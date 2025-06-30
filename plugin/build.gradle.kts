plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
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
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":generator"))
}
