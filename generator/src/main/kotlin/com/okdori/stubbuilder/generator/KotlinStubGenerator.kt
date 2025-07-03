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

            // 본문 템플릿 추가 (기본적으로 NotImplementedError throw)
            funSpecBuilder.addCode(methodInfo.bodyTemplate
                ?: "throw %T(\"Method '${methodInfo.name}' not implemented\")", NotImplementedError::class)

            classBuilder.addFunction(funSpecBuilder.build())
        }

        fileBuilder.addType(classBuilder.build())
        return fileBuilder.build().toString()
    }

    /**
     * 문자열 타입 이름을 KotlinPoet의 TypeName으로 변환하는 헬퍼 함수
     * TODO: 더 복잡한 타입 (제네릭, 배열 등) 처리 로직 필요
     */
    private fun getTypeFromName(typeName: String): TypeName {
        return when (typeName) {
            "String" -> String::class.asTypeName()
            "Int" -> Int::class.asTypeName()
            "Long" -> Long::class.asTypeName()
            "Boolean" -> Boolean::class.asTypeName()
            "Double" -> Double::class.asTypeName()
            "Float" -> Float::class.asTypeName()
            "Unit" -> Unit::class.asTypeName()
            "Any" -> Any::class.asTypeName().copy(nullable = true)
            "List<String>" -> List::class.asTypeName().parameterizedBy(String::class.asTypeName())
            "List<Int>" -> List::class.asTypeName().parameterizedBy(Int::class.asTypeName())
            else -> ClassName(
                typeName.substringBeforeLast('.', ""),
                typeName.substringAfterLast('.')
            )
        }
    }

    /**
     * 주어진 타입에 대한 기본값을 반환하는 헬퍼 함수
     * TODO: 더 복잡한 타입 (컬렉션, 사용자 정의 객체 등) 처리 로직 필요
     */
    private fun getDefaultValueForType(typeName: String): String {
        return when (typeName) {
            "String" -> "\"\""
            "Int", "Long", "Double", "Float" -> "0"
            "Boolean" -> "false"
            "Unit" -> ""
            "Any" -> "null"
            "List<String>", "List<Int>" -> "emptyList()"
            else -> "null" // 기본적으로 null 처리
        }
    }
}
