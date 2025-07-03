package com.okdori.stubbuilder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : KotlinStubGenerator
 * author         : okdori
 * date           : 2025. 7. 3.
 * description    :
 */

class KotlinStubGenerator : StubGenerator {
    override fun generate(stubInfo: StubInfo): String {
        val fileBuilder = FileSpec.builder(stubInfo.packageName, stubInfo.className)
        val classBuilder = TypeSpec.classBuilder(stubInfo.className)
            .addModifiers(KModifier.DATA)
        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        // 필드 추가 - 주생성자
        stubInfo.fields.forEach { fieldInfo ->
            val propertyName = fieldInfo.name
            val propertyType = getTypeFromName(fieldInfo.type)
            val propertyDefaultValue = fieldInfo.defaultValue?.let { CodeBlock.of("%L", it) }
                ?: getDefaultValueForType(fieldInfo.type)

            primaryConstructorBuilder.addParameter(
                ParameterSpec.builder(propertyName, propertyType)
                    .defaultValue("%L", propertyDefaultValue)
                    .build()
            )

            classBuilder.addProperty(PropertySpec.builder(propertyName, propertyType).initializer(propertyName).build())
        }
        classBuilder.primaryConstructor(primaryConstructorBuilder.build())

        // 메서드 추가
        stubInfo.methods.forEach { methodInfo ->
            val funSpecBuilder = FunSpec.builder(methodInfo.name)
                .returns(getTypeFromName(methodInfo.returnType))

            // 파라미터 추가
            methodInfo.parameters.forEach { paramInfo ->
                funSpecBuilder.addParameter(paramInfo.name, getTypeFromName(paramInfo.type))
            }

            // 본문 템플릿 추가
            funSpecBuilder.addCode(methodInfo.bodyTemplate
                ?: getDefaultMethodBodyForType(methodInfo.returnType, methodInfo.name))

            classBuilder.addFunction(funSpecBuilder.build())
        }

        fileBuilder.addType(classBuilder.build())
        return fileBuilder.build().toString().replace("public ", "")
    }

    /**
     * 문자열 타입 이름을 KotlinPoet의 TypeName으로 변환하는 헬퍼 함수
     */
    private fun getTypeFromName(typeName: String): TypeName {
        val nullable = typeName.endsWith("?")
        val cleanTypeName = typeName.removeSuffix("?").trim()

        return when {
            cleanTypeName == "String" -> String::class.asTypeName()
            cleanTypeName == "Int" -> Int::class.asTypeName()
            cleanTypeName == "Long" -> Long::class.asTypeName()
            cleanTypeName == "Boolean" -> Boolean::class.asTypeName()
            cleanTypeName == "Double" -> Double::class.asTypeName()
            cleanTypeName == "Float" -> Float::class.asTypeName()
            cleanTypeName == "Unit" -> Unit::class.asTypeName()
            cleanTypeName == "Any" -> Any::class.asTypeName()
            cleanTypeName.startsWith("List<") -> List::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("MutableList<") -> MutableList::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("Set<") -> Set::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("MutableSet<") -> MutableSet::class.asTypeName().parameterizedBy(getTypeArgument(cleanTypeName))
            cleanTypeName.startsWith("Map<") -> {
                val (keyType, valueType) = getMapTypeArguments(cleanTypeName)
                Map::class.asTypeName().parameterizedBy(keyType, valueType)
            }
            cleanTypeName.startsWith("MutableMap<") -> {
                val (keyType, valueType) = getMapTypeArguments(cleanTypeName)
                MutableMap::class.asTypeName().parameterizedBy(keyType, valueType)
            }
            cleanTypeName == "java.util.Optional" -> ClassName("java.util", "Optional")
            cleanTypeName.startsWith("java.util.Optional<") -> ClassName("java.util", "Optional").parameterizedBy(getTypeArgument(cleanTypeName))
            // 그 외 사용자 정의 클래스 또는 특정 패키지 클래스 처리
            else -> ClassName(
                typeName.substringBeforeLast('.', ""),
                typeName.substringAfterLast('.')
            )
        }.copy(nullable = nullable) // "?" 여부 반영
    }

    /**
     * 제네릭 타입의 인자를 추출하는 헬퍼 함수
     */
    private fun getTypeArgument(fullTypeName: String): TypeName {
        val startIndex = fullTypeName.indexOf('<') + 1
        val endIndex = fullTypeName.lastIndexOf('>')
        if (startIndex == 0 || endIndex == -1 || startIndex >= endIndex) return STAR
        val typeArg = fullTypeName.substring(startIndex, endIndex).trim()
        return getTypeFromName(typeArg)
    }

    /**
     * Map 타입의 키와 값 타입 인자를 추출하는 헬퍼 함수
     */
    private fun getMapTypeArguments(fullTypeName: String): Pair<TypeName, TypeName> {
        val content = fullTypeName.substringAfter('<').substringBeforeLast('>')
        val commaIndex = content.indexOf(',')
        if (commaIndex == -1) return Pair(STAR, STAR)

        val keyType = content.substring(0, commaIndex).trim()
        val valueType = content.substring(commaIndex + 1).trim()
        return Pair(getTypeFromName(keyType), getTypeFromName(valueType))
    }

    /**
     * 주어진 타입에 대한 기본값을 반환하는 헬퍼 함수
     */
    private fun getDefaultValueForType(typeName: String): CodeBlock {
        return when {
            typeName == "String" -> CodeBlock.of("\"\"")
            typeName == "Int" -> CodeBlock.of("0")
            typeName == "Long" -> CodeBlock.of("0L")
            typeName == "Boolean" -> CodeBlock.of("false")
            typeName == "Double" -> CodeBlock.of("0.0")
            typeName == "Float" -> CodeBlock.of("0.0f")
            typeName == "Unit" -> CodeBlock.of("")
            typeName == "Any" -> CodeBlock.of("null")
            typeName.startsWith("List<") || typeName.startsWith("MutableList<*") -> CodeBlock.of("emptyList()")
            typeName.startsWith("Set<") || typeName.startsWith("MutableSet<*") -> CodeBlock.of("emptySet()")
            typeName.startsWith("Map<") || typeName.startsWith("MutableMap<*") -> CodeBlock.of("emptyMap()")
            typeName.startsWith("java.util.Optional") -> CodeBlock.of("%T.empty()", ClassName("java.util", "Optional"))
            typeName.endsWith("?") -> CodeBlock.of("null")
            else -> CodeBlock.of("null")
        }
    }

    /**
     * 메서드 반환 타입에 따른 기본 본문 템플릿 제공 함수
     */
    private fun getDefaultMethodBodyForType(returnType: String, methodName: String): String {
        return when {
            returnType == "Unit" -> "" // Unit 반환형은 본문 없음
            returnType.endsWith("?") || getDefaultValueForType(returnType).toString().trim() == "null" -> "return null"
            returnType == "String" -> "return \"default-$methodName-result\""
            returnType == "Int" -> "return 0"
            returnType == "Long" -> "return 0L"
            returnType == "Boolean" -> "return false"
            returnType == "Double" -> "return 0.0"
            returnType == "Float" -> "return 0.0f"
            returnType.startsWith("List<") || returnType.startsWith("MutableList<*") -> "return emptyList()"
            returnType.startsWith("Set<") || returnType.startsWith("MutableSet<*") -> "return emptySet()"
            returnType.startsWith("Map<") || returnType.startsWith("MutableMap<*") -> "return emptyMap()"
            returnType.startsWith("java.util.Optional") -> CodeBlock.of("return %T.empty()", ClassName("java.util", "Optional")).toString()
            else -> CodeBlock.of("throw %T(\"Method '$methodName' not implemented\")", NotImplementedError::class).toString() // 그 외는 NotImplementedError
        }
    }
}
