package com.okdori.stubbuilder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import io.mockk.mockk
import java.io.File

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : TestCodeGenerator
 * author         : okdori
 * date           : 2025. 6. 30.
 * description    :
 */

/**
 * [StubGenerationInfo]를 기반으로 JUnit 5 및 MockK를 사용하는 테스트 스텁 코드를 생성하는 클래스입니다.
 */
class TestCodeGenerator {

    /**
     * 주어진 [StubGenerationInfo]를 기반으로 테스트 스텁 파일을 생성하고 반환합니다.
     *
     * @param info 테스트 스텁 생성을 위한 정보입니다.
     * @param outputDirPath 생성된 테스트 파일이 저장될 디렉토리 경로입니다.
     * @return 생성된 테스트 스텁 코드의 문자열 표현입니다.
     */
    fun generateTestStub(info: StubGenerationInfo, outputDirPath: String): String {
        println("StubBuilder Generator: '${info.simpleClassName}Test.kt' 코드 생성을 시작합니다.")

        val testClassBuilder = TypeSpec.classBuilder(info.testClassName)
            .addKdoc("StubBuilder에 의해 [%L] 서비스에 대해 생성되었습니다.\n", info.simpleClassName)
            .addKdoc("이 파일은 서비스 테스트를 위한 기본적인 구조를 제공합니다. TODO 주석을 따라가며 실제 비즈니스 로직에 맞게 구현하세요.")

        // Mock 객체 속성 추가
        info.mockProperties.forEach { mockProp ->
            val propType = ClassName(mockProp.type.java.packageName, mockProp.type.simpleName!!)
            testClassBuilder.addProperty(
                PropertySpec.builder(mockProp.name, propType.copy(nullable = true), KModifier.PRIVATE)
                    .initializer("null")
                    .build()
            )
        }
        // 서비스 인스턴스 속성 추가
        testClassBuilder.addProperty(
            PropertySpec.builder(info.serviceInstanceName, info.serviceType, KModifier.PRIVATE)
                .addModifiers(KModifier.LATEINIT)
                .build()
        )

        // @BeforeEach 함수 생성 (Mock 및 서비스 인스턴스 초기화)
        val beforeEachFun = FunSpec.builder("setUp")
            .addAnnotation(ClassName("org.junit.jupiter.api", "BeforeEach"))
            .addKdoc("각 테스트 메서드 실행 전에 Mock 객체와 서비스 인스턴스를 초기화합니다.")
            .addCode("    // Mock 객체 초기화\n")

        info.mockProperties.forEach { mockProp ->
            beforeEachFun.addStatement(
                "    ${mockProp.name} = %T(%L)",
                mockk(),
                if (mockProp.type.java.isInterface) "relaxed = true" else "" // 인터페이스는 relaxed mock으로 기본 설정
            )
        }
        val constructorParamsCode = info.mockProperties.joinToString(", ") { "${it.name}!!" }
        beforeEachFun.addStatement("    ${info.serviceInstanceName} = %T($constructorParamsCode)", info.serviceType)
        testClassBuilder.addFunction(beforeEachFun.build())

        // 각 서비스 메서드에 대한 @Nested 클래스 및 기본 @Test 함수 생성
        info.testMethods.forEach { methodInfo ->
            val nestedClassBuilder = TypeSpec.classBuilder("${methodInfo.functionName.replaceFirstChar { it.uppercaseChar() } }Test")
                .addModifiers(KModifier.INNER)
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("org.junit.jupiter.api", "DisplayName"))
                        .addMember("%S", "${methodInfo.functionName}() 메서드 테스트")
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
                .addKdoc("성공적으로 [${methodInfo.functionName}] 메서드가 실행되는지 검증합니다.")
                .addCode("    // Given: 테스트용 데이터 및 Mocking 설정\n")

            val paramNames = mutableListOf<String>()
            methodInfo.parameters.forEach { param ->
                val paramName = param.name ?: "param"
                paramNames.add(paramName)
                successTestFunBuilder.addStatement("    val %L = %L // TODO: '%L'에 실제 데이터를 할당하세요.", paramName, getDefaultValueForKType(param.type.toString()), paramName)
            }
            successTestFunBuilder.addCode("\n    // TODO: 의존성 Mocking 설정 (예: every { someMock.someMethod() } returns someValue)\n")
            info.mockProperties.forEach { mockProp ->
                successTestFunBuilder.addStatement("    // every { ${mockProp.name}!!.someMethod(any()) } returns /* Mock 응답 */")
            }

            successTestFunBuilder.addCode("\n    // When: 메서드 호출\n")
            successTestFunBuilder.addStatement("    val result = ${info.serviceInstanceName}.${methodInfo.functionName}(${paramNames.joinToString(", ")})")

            successTestFunBuilder.addCode("\n    // Then: 결과 검증\n")
            when {
                methodInfo.returnType.contains("kotlin.Unit") || methodInfo.returnType.contains("void") ->
                    successTestFunBuilder.addStatement("    // 반환 값이 없는 메서드입니다. verify를 통해 동작을 검증하세요.")
                methodInfo.returnType.contains("Optional<") ->
                    successTestFunBuilder.addStatement("    assertTrue(result.isPresent) { \"결과가 Optional.empty()가 아니어야 합니다.\" } // TODO: Optional 내부 값 검증")
                methodInfo.returnType.contains("List<") || methodInfo.returnType.contains("Set<") ->
                    successTestFunBuilder.addStatement("    assertFalse(result.isEmpty()) { \"결과 리스트/세트가 비어있지 않아야 합니다.\" } // TODO: 컬렉션 내용 상세 검증")
                methodInfo.returnType.contains("Boolean") ->
                    successTestFunBuilder.addStatement("    assertTrue(result) { \"결과가 true이어야 합니다.\" } // TODO: 반환값에 따른 정확한 검증")
                else ->
                    successTestFunBuilder.addStatement("    assertNotNull(result) { \"결과가 null이 아니어야 합니다.\" } // TODO: 반환 객체의 속성을 상세히 검증하세요.")
            }

            successTestFunBuilder.addCode("\n    // Verify: Mocked 의존성 메서드 호출 검증\n")
            info.mockProperties.forEach { mockProp ->
                successTestFunBuilder.addStatement("    verify(exactly = 1) { ${mockProp.name}!!.someMethod(any()) } // TODO: ${mockProp.name}의 실제 호출된 메서드와 인자를 지정하세요.")
            }

            nestedClassBuilder.addFunction(successTestFunBuilder.build())

            // 예외 발생 케이스 테스트 메서드 생성 (트랜잭셔널 또는 뮤테이터 메서드에만)
            if (methodInfo.isTransactional || methodInfo.isMutator) {
                val exceptionTestFunBuilder = FunSpec.builder("should_throw_exception_when_${methodInfo.functionName.replaceFirstChar { it.lowercaseChar() } }_fails")
                    .addAnnotation(ClassName("org.junit.jupiter.api", "Test"))
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("org.junit.jupiter.api", "DisplayName"))
                            .addMember("%S", "${methodInfo.functionName} 예외 발생 케이스")
                            .build()
                    )
                    .addKdoc("[$methodInfo.functionName] 메서드 실행 중 예외가 발생하는지 검증합니다.")
                    .addCode("    // Given: 예외 발생을 위한 Mocking 또는 조건 설정\n")
                    .addCode("    // every { someMock.someMethod() } throws SomeException()\n")
                    .addCode("    // TODO: 예외 발생 시나리오에 필요한 Given 절을 작성하세요.\n")

                    .addCode("\n    // When & Then: 예외 발생 검증\n")
                    .addCode("    assertThrows<%T> { // TODO: 여기에 실제 발생할 예외 타입을 지정하세요. (예: IllegalArgumentException::class.java)\n", ClassName("java.lang", "Exception").parameterizedBy(STAR)) // assertThrows에 대한 기본값으로 Exception::class.java 사용
                    .addStatement("        ${info.serviceInstanceName}.${methodInfo.functionName}(${paramNames.joinToString(", ")})")
                    .addCode("    }\n")
                    .addCode("\n    // Verify: Mocked 의존성 메서드 호출 검증 (예외 발생 후 호출되지 않아야 하는 것들 포함)\n")
                info.mockProperties.forEach { mockProp ->
                    exceptionTestFunBuilder.addStatement("    // verify(exactly = 1) { ${mockProp.name}!!.someMethod(any()) } // TODO: 예외 발생 전 호출되는 메서드를 지정하세요.")
                    exceptionTestFunBuilder.addStatement("    // verify(exactly = 0) { ${mockProp.name}!!.someOtherMethod(any()) } // TODO: 예외 발생 후 호출되지 않아야 할 메서드를 지정하세요.")
                }

                nestedClassBuilder.addFunction(exceptionTestFunBuilder.build())
            }
            testClassBuilder.addType(nestedClassBuilder.build())
        }

        // 생성된 Kotlin 코드를 파일로 저장
        val fileSpec = FileSpec.builder(info.packageName, info.testClassName)
            .addType(testClassBuilder.build())
            .addImportsForTestFile() // 헬퍼 함수로 임포트 정리
            .build()

        val outputDirFile = File(outputDirPath)
        outputDirFile.mkdirs()
        fileSpec.writeTo(outputDirFile)

        println("StubBuilder Generator: '${info.testClassName}.kt' 파일이 '${outputDirFile.absolutePath}' 경로에 성공적으로 생성되었습니다.")
        return fileSpec.toString()
    }

    /**
     * Kotlin Poetik의 TypeName으로 변환하는 헬퍼 함수
     * `KotlinStubGenerator`의 getTypeFromName()과 동일하게 사용될 수 있으나,
     * 여기서는 KType.toString()으로 받은 문자열을 처리하도록 합니다.
     * 실제 KType을 직접 파라미터로 받는 것이 더 견고합니다.
     */
    private fun getTypeFromName(typeName: String): TypeName {
        val nullable = typeName.endsWith("?")
        val cleanTypeName = typeName.removeSuffix("?").trim()

        return when {
            cleanTypeName == "kotlin.String" -> String::class.asTypeName()
            cleanTypeName == "kotlin.Int" -> Int::class.asTypeName()
            cleanTypeName == "kotlin.Long" -> Long::class.asTypeName()
            cleanTypeName == "kotlin.Boolean" -> Boolean::class.asTypeName()
            cleanTypeName == "kotlin.Double" -> Double::class.asTypeName()
            cleanTypeName == "kotlin.Float" -> Float::class.asTypeName()
            cleanTypeName == "kotlin.Unit" -> Unit::class.asTypeName()
            cleanTypeName == "kotlin.Any" -> Any::class.asTypeName()
            cleanTypeName.startsWith("kotlin.collections.List<") -> List::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("kotlin.collections.MutableList<") -> MutableList::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("kotlin.collections.Set<") -> Set::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("kotlin.collections.MutableSet<") -> MutableSet::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("kotlin.collections.Map<") -> {
                val (keyType, valueType) = getMapTypeArguments(cleanTypeName)
                Map::class.asTypeName().parameterizedBy(keyType, valueType)
            }
            cleanTypeName.startsWith("kotlin.collections.MutableMap<") -> {
                val (keyType, valueType) = getMapTypeArguments(cleanTypeName)
                MutableMap::class.asTypeName().parameterizedBy(keyType, valueType)
            }
            cleanTypeName == "java.util.Optional" -> ClassName("java.util", "Optional")
            cleanTypeName.startsWith("java.util.Optional<") -> ClassName("java.util", "Optional").parameterizedBy(getTypeArgument(cleanTypeName))
            else -> {
                // FQCN 파싱: 마지막 점을 기준으로 패키지와 클래스명 분리
                val lastDotIndex = cleanTypeName.lastIndexOf('.')
                if (lastDotIndex == -1) {
                    // 패키지가 없는 경우 (예: "MyClass")
                    ClassName("", cleanTypeName)
                } else {
                    val pkg = cleanTypeName.substring(0, lastDotIndex)
                    val simpleName = cleanTypeName.substring(lastDotIndex + 1)
                    ClassName(pkg, simpleName)
                }
            }
        }.copy(nullable = nullable) // "?" 여부 반영
    }

    private fun getTypeArgument(fullTypeName: String): TypeName {
        val startIndex = fullTypeName.indexOf('<') + 1
        val endIndex = fullTypeName.lastIndexOf('>')
        if (startIndex == 0 || endIndex == -1 || startIndex >= endIndex) return STAR
        val typeArg = fullTypeName.substring(startIndex, endIndex).trim()
        return getTypeFromName(typeArg)
    }

    private fun getMapTypeArguments(fullTypeName: String): Pair<TypeName, TypeName> {
        val content = fullTypeName.substringAfter('<').substringBeforeLast('>')
        val commaIndex = content.indexOf(',')
        if (commaIndex == -1) return Pair(STAR, STAR)

        val keyType = content.substring(0, commaIndex).trim()
        val valueType = content.substring(commaIndex + 1).trim()
        return Pair(getTypeFromName(keyType), getTypeFromName(valueType))
    }

    /**
     * 주어진 KType 문자열에 대한 기본값을 반환하는 헬퍼 함수
     * KType.toString() 결과에 의존하므로, 정확한 타입 매핑이 중요합니다.
     */
    private fun getDefaultValueForKType(typeName: String): String {
        return when {
            typeName == "kotlin.String" -> "\"test-value\""
            typeName == "kotlin.Int" -> "0"
            typeName == "kotlin.Long" -> "0L"
            typeName == "kotlin.Boolean" -> "false"
            typeName == "kotlin.Double" -> "0.0"
            typeName == "kotlin.Float" -> "0.0f"
            typeName == "kotlin.Unit" -> ""
            typeName == "kotlin.Any" -> "null"
            typeName.startsWith("kotlin.collections.List<") || typeName.startsWith("kotlin.collections.MutableList<") -> "emptyList()"
            typeName.startsWith("kotlin.collections.Set<") || typeName.startsWith("kotlin.collections.MutableSet<") -> "emptySet()"
            typeName.startsWith("kotlin.collections.Map<") || typeName.startsWith("kotlin.collections.MutableMap<") -> "emptyMap()"
            typeName.startsWith("java.util.Optional<") -> "Optional.empty()"
            typeName.endsWith("?") -> "null" // Nullable 타입은 기본적으로 null
            else -> "// TODO: ${typeName} 타입의 객체를 초기화하세요." // 사용자 정의 타입이나 복잡한 타입은 TODO로 남김
        }
    }
}

// FileSpec 확장 함수로 임포트 추가를 캡슐화
fun FileSpec.Builder.addImportsForTestFile(): FileSpec.Builder {
    addImport("io.mockk", "mockk", "every", "verify", "just", "Runs", "any")
    addImport("org.junit.jupiter.api", "Test", "BeforeEach", "DisplayName", "Nested")
    addImport("org.junit.jupiter.api.Assertions", "assertTrue", "assertFalse", "assertNotNull", "assertThrows")
    addImport("java.util", "Optional")
    // 참고: 리플렉션 관련 임포트는 생성된 테스트 파일 자체에는 필요 없고, 분석기에만 필요합니다.
    // addImport("kotlin.reflect.full", "declaredFunctions", "primaryConstructor", "findAnnotation")
    // addImport("kotlin.reflect", "KClass", "KFunction", "KParameter", "KVisibility")
    addImport("org.springframework.stereotype", "Service") // Mock 객체에 어노테이션이 필요하다면 (일반적으로 테스트 파일 자체에는 불필요)
    addImport("org.springframework.transaction.annotation", "Transactional") // 위와 동일
    return this
}
