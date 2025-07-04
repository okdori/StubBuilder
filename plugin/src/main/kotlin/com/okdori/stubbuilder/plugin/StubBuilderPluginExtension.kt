package com.okdori.stubbuilder.plugin

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * StubBuilder Gradle 플러그인의 설정을 위한 확장 클래스입니다.
 */
abstract class StubBuilderPluginExtension {
    /**
     * 테스트 스텁을 생성할 서비스 클래스 이름들의 리스트입니다.
     * 예: `listOf("com.example.myapp.service.UserService", "com.example.myapp.service.ProductService")`
     */
    @get:Input
    abstract val serviceClasses: Property<List<String>>

    /**
     * 생성된 테스트 스텁 파일이 저장될 출력 디렉토리 이름입니다.
     * 기본값은 `generated/stubbuilder/tests`입니다.
     */
    @get:Input
    abstract val outputDirName: Property<String>

    init {
        // 기본값 설정
        outputDirName.convention("generated/stubbuilder/tests")
        serviceClasses.convention(emptyList()) // 기본적으로 비어 있는 리스트
    }
}
