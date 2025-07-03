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

        // 필드 추가
        stubInfo.fields.forEach { fieldInfo ->
            val propertySpecBuilder = PropertySpec.builder(
                name = fieldInfo.name,
                type = getTypeFromName(fieldInfo.type)
            ).initializer(fieldInfo.defaultValue ?: getDefaultValueForType(fieldInfo.type))

            classBuilder.addProperty(propertySpecBuilder.build())
        }

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
        return fileBuilder.build().toString()
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
    private fun getDefaultValueForType(typeName: String): String {
        return when {
            typeName == "String" -> "\"\""
            typeName == "Int" -> "0"
            typeName == "Long" -> "0L"
            typeName == "Boolean" -> "false"
            typeName == "Double" -> "0.0"
            typeName == "Float" -> "0.0f"
            typeName == "Unit" -> ""
            typeName == "Any" -> "null"
            typeName.startsWith("List<") || typeName.startsWith("MutableList<*") -> "emptyList()"
            typeName.startsWith("Set<") || typeName.startsWith("MutableSet<*") -> "emptySet()"
            typeName.startsWith("Map<") || typeName.startsWith("MutableMap<*") -> "emptyMap()"
            typeName.startsWith("java.util.Optional") -> "%T.empty()".format(ClassName("java.util", "Optional"))
            typeName.endsWith("?") -> "null"
            else -> "null"
        }
    }

    /**
     * 메서드 반환 타입에 따른 기본 본문 템플릿 제공 함수
     */
    private fun getDefaultMethodBodyForType(returnType: String, methodName: String): String {
        return when {
            returnType == "Unit" -> "" // Unit 반환형은 본문 없음
            returnType.endsWith("?") || getDefaultValueForType(returnType) == "null" -> "return null" // nullable 또는 기본값이 null인 경우
            returnType == "String" -> "return \"default-$methodName-result\""
            returnType == "Int" -> "return 0"
            returnType == "Long" -> "return 0L"
            returnType == "Boolean" -> "return false"
            returnType == "Double" -> "return 0.0"
            returnType == "Float" -> "return 0.0f"
            returnType.startsWith("List<") || returnType.startsWith("MutableList<*") -> "return emptyList()"
            returnType.startsWith("Set<") || returnType.startsWith("MutableSet<*") -> "return emptySet()"
            returnType.startsWith("Map<") || returnType.startsWith("MutableMap<*") -> "return emptyMap()"
            returnType.startsWith("java.util.Optional") -> "return %T.empty()".format(ClassName("java.util", "Optional"))
            else -> "throw %T(\"Method '$methodName' not implemented\")".format(NotImplementedError::class) // 그 외는 NotImplementedError
        }
    }
}
