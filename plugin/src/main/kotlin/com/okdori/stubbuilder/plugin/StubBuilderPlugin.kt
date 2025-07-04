package com.okdori.stubbuilder.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

/**
 * StubBuilder Gradle 플러그인의 메인 클래스입니다.
 * 이 플러그인은 Spring `@Service` 및 `@Transactional` 어노테이션이 붙은 클래스에 대한
 * MockK 기반의 JUnit 5 테스트 스텁을 자동으로 생성합니다.
 */
class StubBuilderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 플러그인 확장 등록 (사용자가 build.gradle에서 stubBuilder {} 블록을 사용할 수 있도록)
        val extension = project.extensions.create("stubBuilder", StubBuilderPluginExtension::class.java)

        // GenerateStubsTask 등록
        val generateStubsTask = project.tasks.register("generateStubs", GenerateStubsTask::class.java) {
            serviceClasses.set(extension.serviceClasses)
            outputDirectory.set(project.layout.buildDirectory.dir(extension.outputDirName))

            // 대상 프로젝트의 'main' 소스셋 컴파일 클래스패스를 태스크에 연결합니다.
            // Java (Kotlin JVM 프로젝트도 JavaPlugin을 통해 sourceSets를 가짐)
            project.plugins.withType(org.gradle.api.plugins.JavaPlugin::class.java) {
                val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
                val mainSourceSet = javaExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                compilationClasspath.set(mainSourceSet.runtimeClasspath) // 리플렉션을 위해 runtimeClasspath가 더 포괄적입니다.
                // 또는 컴파일된 클래스만 필요하다면:
                // task.compilationClasspath.set(project.files(mainSourceSet.output.classesDirs, mainSourceSet.runtimeClasspath))
            }

            // 생성된 스텁 소스 코드를 테스트 소스셋에 추가하여 IDE가 인식하고 컴파일하도록 합니다.
            project.afterEvaluate { // 프로젝트가 다른 플러그인에 의해 완전히 구성된 후에 실행되도록 합니다.
                project.plugins.withType(org.gradle.api.plugins.JavaPlugin::class.java) {
                    val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
                    val testSourceSet = javaExtension.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)

                    // Kotlin 소스셋에도 추가
                    val kotlinSourceSets = project.extensions.findByType(KotlinSourceSetContainer::class.java)
                    val kotlinTestSourceSet = kotlinSourceSets?.sourceSets?.findByName(SourceSet.TEST_SOURCE_SET_NAME)

                    // 스텁 파일이 생성될 실제 디렉토리 경로를 가져옵니다.
                    val generatedStubsDir = project.layout.buildDirectory.dir(extension.outputDirName.get()).get().asFile

                    // 생성된 디렉토리를 테스트 소스셋에 추가합니다.
                    testSourceSet.java.srcDir(generatedStubsDir) // Java 프로젝트용
                    kotlinTestSourceSet?.kotlin?.srcDir(generatedStubsDir)

                    // 테스트 태스크가 생성된 코드를 컴파일하도록 보장합니다.
                    project.tasks.getByName("compileTestKotlin").dependsOn(this)
                    project.tasks.getByName("compileTestJava").dependsOn(this)
                    project.tasks.getByName("test").dependsOn(this)
                }
            }
        }
    }
}
