package com.okdori.stubbuilder.generator

import org.junit.jupiter.api.Test
import java.io.File

class TestStubGeneratorManualTest {

    @Test
    fun `should generate test stub for SampleUserService directly`() {
        val serviceClassName = "com.okdori.stubbuilder.test.SampleUserService"

        // 2. 스텁 파일이 생성될 출력 디렉토리 (build 디렉토리 하위의 generated-test-sources)
        // 이 경로를 Gradle이 인식할 수 있도록 build.gradle.kts에 추가할 것입니다.
        val outputDirPath = "build/generated/test/kotlin" // <--- 이 경로를 수정합니다.
        val outputFile = File(outputDirPath)
        outputFile.mkdirs() // 디렉토리 생성

        println("\n--- TestStubGenerator 직접 실행 테스트 시작 ---")
        println("대상 서비스: $serviceClassName")
        println("생성될 디렉토리: ${outputFile.absolutePath}")

        val generator = TestStubGenerator(this.javaClass.classLoader)

        try {
            val generatedCode = generator.generateTestStub(serviceClassName, outputDirPath)
            println("\n--- 스텁 코드 생성 성공 ---")
            println("생성된 코드 미리보기:\n")
            println(generatedCode.substring(0, Math.min(generatedCode.length, 500)))
            println("\n... (전체 코드는 ${outputFile.absolutePath} 디렉토리에서 확인하세요)")

            // 생성된 파일 경로 검증 (패키지 구조를 포함해야 합니다)
            val expectedFilePath = "${outputDirPath}/com/okdori/stubbuilder/test/SampleUserServiceTest.kt"
            val generatedFile = File(expectedFilePath)
            assert(generatedFile.exists()) { "생성된 파일이 존재하지 않습니다: $expectedFilePath" }
            assert(generatedFile.readText().contains("class SampleUserServiceTest")) { "생성된 파일 내용이 올바르지 않습니다." }

            println("\n--- TestStubGenerator 직접 실행 테스트 성공 ---")

        } catch (e: Exception) {
            System.err.println("\n--- 스텁 코드 생성 실패 ---")
            e.printStackTrace()
            throw e
        }
    }
}
