package com.okdori.stubbuilder.plugin

import com.okdori.stubbuilder.generator.analyzer.ServiceClassAnalyzer
import com.okdori.stubbuilder.generator.TestCodeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Classpath // 클래스패스를 구성하는 파일들을 위해 @Classpath 사용
import org.gradle.api.provider.Property

/**
 * packageName    : com.okdori.stubbuilder.plugin
 * fileName       : GenerateStubsTask
 * author         : okdori
 * date           : 2025. 7. 3.
 * description    :
 */

abstract class GenerateStubsTask : DefaultTask() {

    @get:Input
    abstract val serviceClasses: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    // 태스크의 입력을 올바르게 선언하기 위해 @InputFiles와 @Classpath를 사용합니다.
    // 이는 Gradle에게 이 파일들이 태스크의 입력임을 알려주어,
    // 증분 빌드와 적절한 캐싱을 가능하게 합니다.
    @get:InputFiles
    @get:Classpath // 이 파일들이 클래스패스를 구성함을 나타냅니다.
    abstract val compilationClasspath: Property<FileCollection> // 컴파일된 클래스와 의존성을 나타냅니다.

    init {
        description = "Spring @Service 및 @Transactional 클래스에 대한 MockK 기반 테스트 스텁을 생성합니다."
        group = "stubbuilder"
    }

    @TaskAction
    fun generate() {
        val servicesToGenerate = serviceClasses.get()
        val outputDir = outputDirectory.asFile.get()
        val classPathFiles = compilationClasspath.get().files.toList()

        println("=========================================================")
        println("       StubBuilder 플러그인 실행 중...                   ")
        println("---------------------------------------------------------")
        println("  대상 서비스 클래스 수: ${servicesToGenerate.size}")
        println("  출력 경로: ${outputDir.absolutePath}")
        println("=========================================================")

        if (servicesToGenerate.isEmpty()) {
            logger.warn("StubBuilder: 'serviceClasses'가 설정되지 않았습니다. 생성할 스텁이 없습니다.")
            return
        }

        val analyzer = ServiceClassAnalyzer(classPathFiles)
        val generator = TestCodeGenerator()

        servicesToGenerate.forEach { serviceClassName ->
            try {
                val stubInfo = analyzer.analyze(serviceClassName)
                generator.generateTestStub(stubInfo, outputDir.absolutePath)
            } catch (e: IllegalArgumentException) {
                logger.error("StubBuilder 오류: ${e.message}", e)
                // 선호도에 따라 빌드를 실패시키려면 여기서 throw 할 수 있습니다:
                // throw GradleException("Stub generation failed for $serviceClassName: ${e.message}", e)
            } catch (e: Exception) {
                logger.error("StubBuilder: '$serviceClassName' 스텁 생성 중 알 수 없는 오류 발생", e)
                // throw GradleException("Stub generation failed for $serviceClassName: ${e.message}", e)
            }
        }
        println("StubBuilder: 모든 테스트 스텁 생성이 완료되었습니다.")
    }
}
