package com.okdori.stubbuilder.generator

import com.okdori.stubbuilder.common.annotation.DataMutator
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.AnnotationSpec

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : TestStubGenerator
 * author         : okdori
 * date           : 2025. 6. 30.
 * description    :
 */

/**
 * 서비스 클래스를 분석하여 JUnit 5와 MockK 기반의 테스트 스텁 코드를 생성하는 클래스
 *
 * 이 클래스는 주어진 서비스 클래스의 생성자 의존성과 public 메서드를 파싱하여
 * 테스트 클래스의 구조, Mock 객체 초기화, @BeforeEach 설정, 각 메서드에 대한
 * 성공 및 예외 케이스 테스트 스텁을 생성합니다.
 *
 * @param classLoader 테스트 스텁을 생성할 대상 서비스 클래스를 로드하는 데 사용될 ClassLoader
 */
class TestStubGenerator(private val classLoader: ClassLoader) {

    /**
     * 서비스 클래스에 대한 테스트 스텁 파일을 생성
     *
     * @param serviceClassName 테스트 스텁을 생성할 서비스 클래스의 완전한 이름 (FQCN)
     * @param outputDirPath 생성된 테스트 파일이 저장될 디렉토리 경로
     * @return 생성된 테스트 스텁 코드의 문자열 표현
     * @throws IllegalArgumentException 서비스 클래스를 찾을 수 없거나 @Service 어노테이션이 없는 경우 발생
     */
    fun generateTestStub(serviceClassName: String, outputDirPath: String): String {
        println("StubBuilder: '$serviceClassName' 서비스에 대한 테스트 스텁 생성을 시작합니다.")

        // 대상 서비스 클래스 로드 및 유효성 검증
        val serviceClass = try {
            classLoader.loadClass(serviceClassName).kotlin
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("서비스 클래스 '$serviceClassName'를 클래스패스에서 찾을 수 없습니다. 프로젝트가 빌드되었는지 확인하십시오.", e)
        }

        if (serviceClass.findAnnotation<org.springframework.stereotype.Service>() == null) {
            throw IllegalArgumentException("클래스 '$serviceClassName'는 테스트 스텁 생성을 위해 @Service 어노테이션이 선언되어야 합니다.")
        }

        val packageName = serviceClass.java.packageName
        val simpleClassName = serviceClass.simpleName
        val testClassName = "${simpleClassName}Test"
        val serviceInstanceName = simpleClassName!!.replaceFirstChar { it.lowercaseChar() }


        // 서비스 클래스의 생성자 의존성 분석 (Mocking 대상 식별)
        val constructorParams = serviceClass.primaryConstructor?.parameters ?: emptyList()
        val mockProperties = mutableListOf<MockProperty>()
        constructorParams.forEach { param ->
            val paramName = param.name ?: "unknownParam"
            val paramType = param.type.classifier as? KClass<*>
                ?: throw IllegalArgumentException("서비스 클래스 '$serviceClassName'의 생성자 파라미터 '${param.name}'의 타입을 결정할 수 없습니다.")
            mockProperties.add(MockProperty(paramName, paramType))
        }

        // 서비스 클래스의 Public 메서드 분석 (테스트 케이스 생성 정보 추출)
        val publicFunctions = serviceClass.declaredFunctions.filter {
            it.visibility == KVisibility.PUBLIC && it.name != "equals" && it.name != "hashCode" && it.name != "toString"
        }
        val testMethods = publicFunctions.map { function ->
            val isMutatorByAnnotation = function.findAnnotation<DataMutator>() != null
            val isMutatorByNameConvention = function.name.startsWith("create") ||
                    function.name.startsWith("update") ||
                    function.name.startsWith("delete") ||
                    function.name.startsWith("add") ||
                    function.name.startsWith("remove")

            TestMethodInfo(
                functionName = function.name,
                parameters = function.parameters.filter { it.kind == KParameter.Kind.VALUE },
                returnType = function.returnType.toString(),
                isTransactional = function.findAnnotation<org.springframework.transaction.annotation.Transactional>() != null,
                isMutator = isMutatorByAnnotation || isMutatorByNameConvention
            )
        }

        // KotlinPoet을 이용한 테스트 클래스 빌드 시작
        val testClassBuilder = TypeSpec.classBuilder(testClassName)
            .addKdoc("StubBuilder에 의해 [%L] 서비스에 대해 생성되었습니다.", simpleClassName)

        // Mock 객체 속성 추가 (예: private var _Repository: _Repository? = null)
        mockProperties.forEach { mockProp ->
            val propType = ClassName(mockProp.type.java.packageName, mockProp.type.simpleName!!)
            testClassBuilder.addProperty(
                PropertySpec.builder(mockProp.name, propType.copy(nullable = true), KModifier.PRIVATE)
                    .initializer("null")
                    .build()
            )
        }
        // 서비스 인스턴스 속성 추가 (예: private lateinit var _Service: _Service)
        val serviceType = ClassName(packageName, simpleClassName)
        testClassBuilder.addProperty(
            PropertySpec.builder(serviceInstanceName, serviceType, KModifier.PRIVATE)
                .addModifiers(KModifier.LATEINIT)
                .build()
        )

        // @BeforeEach 함수 생성 (Mock 및 서비스 인스턴스 초기화)
        val beforeEachFun = FunSpec.builder("setUp")
            .addAnnotation(ClassName("org.junit.jupiter.api", "BeforeEach"))
            .addCode("// Mock 객체 초기화\n")
        mockProperties.forEach { mockProp ->
            beforeEachFun.addStatement(
                "${mockProp.name} = %T(%L)",
                ClassName("io.mockk", "mockk"),
                if (mockProp.type.java.isInterface) "relaxed = true" else ""
            )
        }
        val constructorParamsCode = mockProperties.joinToString(", ") { "${it.name}!!" }
        beforeEachFun.addStatement("$serviceInstanceName = %T($constructorParamsCode)", serviceType)
        testClassBuilder.addFunction(beforeEachFun.build())

        // 각 서비스 메서드에 대한 @Nested 클래스 및 기본 @Test 함수 생성
        testMethods.forEach { methodInfo ->
            val nestedClassBuilder = TypeSpec.classBuilder("${methodInfo.functionName.replaceFirstChar { it.uppercaseChar() } }Test")
                .addModifiers(KModifier.INNER)
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("org.junit.jupiter.api", "DisplayName"))
                        .addMember("%S", "${methodInfo.functionName}() 메서드")
                        .build()
                )

            // 성공 케이스 테스트 메서드 생성
            val successTestFunBuilder = FunSpec.builder("should_successfully_${methodInfo.functionName.replaceFirstChar { it.lowercaseChar() } }")
                .addAnnotation(ClassName("org.junit.jupiter.api", "Test"))
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("org.junit.jupiter.api", "DisplayName"))
                        .addMember("%S", "${methodInfo.functionName} 성공 케이스")
                        .build()
                )
                .addCode("// Given: 테스트용 데이터 및 Mocking 설정\n")

            val paramNames = mutableListOf<String>()
            methodInfo.parameters.forEach { param ->
                val paramName = param.name ?: "param"
                paramNames.add(paramName)
                when (param.type.toString()) {
                    "kotlin.Long" -> successTestFunBuilder.addStatement("val %L = %LL", paramName, 1)
                    "kotlin.String" -> successTestFunBuilder.addStatement("val %L = %S", paramName, "test-value-$paramName")
                    "kotlin.Boolean" -> successTestFunBuilder.addStatement("val %L = %L", paramName, true)
                    "kotlin.collections.List<*>", "kotlin.collections.MutableList<*>" ->
                        successTestFunBuilder.addStatement("val %L = %T()", paramName, ClassName("kotlin.collections", "emptyList"))
                    "kotlin.collections.Set<*>", "kotlin.collections.MutableSet<*>" ->
                        successTestFunBuilder.addStatement("val %L = %T()", paramName, ClassName("kotlin.collections", "emptySet"))
                    "kotlin.collections.Map<*, *>", "kotlin.collections.MutableMap<*, *>" ->
                        successTestFunBuilder.addStatement("val %L = %T()", paramName, ClassName("kotlin.collections", "emptyMap"))
                    "java.util.Optional<*>" ->
                        successTestFunBuilder.addStatement("val %L = %T.empty()", paramName, ClassName("java.util", "Optional"))
                    else -> {
                        val paramClassName = (param.type.classifier as? KClass<*>)?.simpleName ?: "Any"
                        successTestFunBuilder.addStatement("val %L = %L(...) // TODO: '%L' 타입의 객체를 초기화하세요.", paramName, paramClassName, paramName)
                    }
                }
            }
            successTestFunBuilder.addCode("    // ... (의존성 Mocking 코드 제시)\n")
            mockProperties.forEach { mockProp ->
                if (mockProp.type.simpleName?.contains("Repository") == true) {
                    successTestFunBuilder.addStatement("    every { %L!!.findById(any()) } returns Optional.of( /* TODO: 반환할 객체 */ )", mockProp.name)
                    successTestFunBuilder.addStatement("    every { %L!!.save(any()) } returns (/* TODO: 저장된 객체 */)", mockProp.name)
                } else if (mockProp.type.simpleName?.contains("Service") == true) {
                    successTestFunBuilder.addStatement("    every { %L!!.someServiceMethod(any()) } just Runs", mockProp.name)
                }
            }
            successTestFunBuilder.addCode("\n// When: 메서드 호출\n")
            successTestFunBuilder.addStatement("val result = ${serviceInstanceName}.${methodInfo.functionName}(${paramNames.joinToString(", ")})")

            successTestFunBuilder.addCode("\n// Then: 결과 검증\n")
            when {
                methodInfo.returnType.contains("Optional<") -> successTestFunBuilder.addStatement("assert(result.isPresent) // TODO: Optional 내부 값 검증")
                methodInfo.returnType.contains("List<") -> successTestFunBuilder.addStatement("assert(result.isNotEmpty()) // TODO: 리스트 내용 검증")
                methodInfo.returnType.contains("Boolean") -> successTestFunBuilder.addStatement("assert(result == true) // TODO: 반환값에 따른 정확한 검증")
                methodInfo.returnType == "kotlin.Unit" || methodInfo.returnType == "void" -> successTestFunBuilder.addStatement("/* 직접적인 반환 값이 없는 메서드 */")
                else -> successTestFunBuilder.addStatement("assert(result != null) // TODO: 반환 객체의 속성을 상세히 검증하세요.")
            }

            successTestFunBuilder.addCode("\n// Verify: Mocked 의존성 메서드 호출 검증\n")
            mockProperties.forEach { mockProp ->
                successTestFunBuilder.addStatement("verify(exactly = 1) { %L!!.someMethod(any()) } // TODO: %L의 실제 호출된 메서드와 인자를 지정하세요.", mockProp.name, mockProp.name)
            }

            nestedClassBuilder.addFunction(successTestFunBuilder.build())

            // 예외 발생 케이스 테스트 메서드 생성
            if (methodInfo.isTransactional || methodInfo.isMutator) {
                val exceptionTestFunBuilder = FunSpec.builder("should_throw_exception_when_${methodInfo.functionName.replaceFirstChar { it.lowercaseChar() } }_fails")
                    .addAnnotation(ClassName("org.junit.jupiter.api", "Test"))
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("org.junit.jupiter.api", "DisplayName"))
                            .addMember("%S", "${methodInfo.functionName} 예외 발생 케이스")
                            .build()
                    )
                    .addCode("// Given: 예외 발생을 위한 Mocking 또는 조건 설정\n")
                    .addCode("    every { %L!!.findById(any()) } returns Optional.empty() // 예외 상황 Mocking 예시\n", mockProperties.firstOrNull()?.name ?: "someRepo")

                    .addCode("\n// When & Then: 예외 발생 검증\n")
                    .addCode("assertThrows<%T> { // TODO: 여기에 실제 발생할 예외 타입을 지정하세요.\n", ClassName("java.lang", "NoSuchElementException"))
                    .addStatement("    ${serviceInstanceName}.${methodInfo.functionName}(${paramNames.joinToString(", ")})")
                    .addCode("}\n")
                    .addCode("\n// Verify: Mocked 의존성 메서드 호출 검증 (예외 발생 후 호출되지 않아야 하는 것들 포함)\n")
                mockProperties.forEach { mockProp ->
                    exceptionTestFunBuilder.addStatement("verify(exactly = 1) { %L!!.someMethod(any()) } // TODO: 예외 발생 전 호출되는 메서드를 지정하세요.", mockProp.name)
                    exceptionTestFunBuilder.addStatement("verify(exactly = 0) { %L!!.someOtherMethod(any()) } // TODO: 예외 발생 후 호출되지 않아야 할 메서드를 지정하세요.", mockProp.name)
                }

                nestedClassBuilder.addFunction(exceptionTestFunBuilder.build())
            }
            testClassBuilder.addType(nestedClassBuilder.build())
        }

        // 생성된 Kotlin 코드를 파일로 저장
        val fileSpec = FileSpec.builder(packageName, testClassName)
            .addType(testClassBuilder.build())
            .addImport("io.mockk", "mockk", "every", "verify", "just", "Runs", "any")
            .addImport("org.junit.jupiter.api", "Test", "BeforeEach", "DisplayName", "Nested")
            .addImport("org.junit.jupiter.api.Assertions", "assertThrows", "assert")
            .addImport("java.util", "Optional")
            .addImport("kotlin.reflect.full", "declaredFunctions", "primaryConstructor", "findAnnotation")
            .addImport("kotlin.reflect", "KClass", "KFunction", "KParameter", "KVisibility")
            .addImport("org.springframework.stereotype", "Service")
            .addImport("org.springframework.transaction.annotation", "Transactional")
            .build()

        val outputDirFile = File(outputDirPath)
        outputDirFile.mkdirs()
        fileSpec.writeTo(outputDirFile)

        println("StubBuilder: '$testClassName.kt' 파일이 '${outputDirFile.absolutePath}' 경로에 성공적으로 생성되었습니다.")
        return fileSpec.toString()
    }
}
