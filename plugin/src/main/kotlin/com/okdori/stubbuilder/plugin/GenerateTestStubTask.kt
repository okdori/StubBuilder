package com.okdori.stubbuilder.plugin

import com.okdori.stubbuilder.generator.TestStubGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.net.URLClassLoader

/**
 * packageName    : com.okdori.stubbuilder.plugin
 * fileName       : GenerateTestStubTask
 * author         : okdori
 * date           : 2025. 6. 30.
 * description    :
 */
abstract class GenerateTestStubTask : DefaultTask() {
    @get:Input
    @get:Option(option = "target-service", description = "테스트 스텁을 생성할 서비스 클래스의 FQCN")
    abstract val targetService: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Option(option = "classpath", description = "서비스 클래스를 로드하는 데 사용될 클래스패스(일반적으로 Gradle에 의해 자동 전달)")
    abstract val classpath: Property<String>


    @TaskAction
    fun generate() {
        val serviceClassName = targetService.get()
        val outputDirectoryPath = outputDir.asFile.get().absolutePath
        val classpathString = classpath.get()

        println("=========================================================")
        println("       StubBuilder 플러그인 실행 중...                   ")
        println("---------------------------------------------------------")
        println("  대상 서비스: $serviceClassName")
        println("  출력 경로: $outputDirectoryPath")
        println("=========================================================")

        val urls = classpathString.split(File.pathSeparator).map { File(it).toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, javaClass.classLoader)

        val generator = TestStubGenerator(classLoader)

        try {
            generator.generateTestStub(serviceClassName, outputDirectoryPath)
            println("StubBuilder: 테스트 스텁 생성이 완료되었습니다.")
        } catch (e: IllegalArgumentException) {
            logger.error("StubBuilder 실행 중 오류 발생: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("StubBuilder 실행 중 알 수 없는 오류 발생", e)
            throw e
        }
    }
}
