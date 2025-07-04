package com.okdori.stubbuilder.generator.analyzer

import com.okdori.stubbuilder.common.annotation.DataMutator
import com.okdori.stubbuilder.generator.MockProperty
import com.okdori.stubbuilder.generator.StubGenerationInfo
import com.okdori.stubbuilder.generator.TestMethodInfo
import com.squareup.kotlinpoet.ClassName
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * packageName    : com.okdori.stubbuilder.generator.analyzer
 * fileName       : ServiceClassAnalyzer
 * author         : okdori
 * date           : 2025. 7. 3.
 * description    :
 */

/**
 * 주어진 서비스 클래스를 분석하여 테스트 스텁 생성에 필요한 정보를 추출하는 클래스입니다.
 *
 * @param classPathUrls 분석할 클래스를 로드할 ClassLoader에 사용될 URL 목록입니다.
 */
class ServiceClassAnalyzer(private val classPathUrls: List<File>) {

    private val classLoader: URLClassLoader = URLClassLoader(
        classPathUrls.map { it.toURI().toURL() }.toTypedArray(),
        this.javaClass.classLoader
    )

    /**
     * 서비스 클래스를 로드하고 분석하여 [StubGenerationInfo] 객체를 반환합니다.
     *
     * @param serviceClassName 분석할 서비스 클래스의 완전한 이름 (FQCN)
     * @return 서비스 클래스의 분석 결과를 담은 [StubGenerationInfo] 객체
     * @throws IllegalArgumentException 서비스 클래스를 찾을 수 없거나 @Service 어노테이션이 없는 경우 발생
     */
    fun analyze(serviceClassName: String): StubGenerationInfo {
        println("StubBuilder Analyzer: '$serviceClassName' 서비스 클래스 분석을 시작합니다.")

        // 대상 서비스 클래스 로드 및 유효성 검증
        val serviceClass = try {
            classLoader.loadClass(serviceClassName).kotlin
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("서비스 클래스 '$serviceClassName'를 클래스패스에서 찾을 수 없습니다. 프로젝트가 빌드되었는지 확인하십시오.", e)
        } catch (e: NoClassDefFoundError) {
            throw IllegalArgumentException("서비스 클래스 '$serviceClassName'의 의존성 중 일부를 찾을 수 없습니다. ClassPath 설정을 확인하십시오.", e)
        }

        // @Service 어노테이션 검증
        if (serviceClass.findAnnotation<Service>() == null) {
            throw IllegalArgumentException("클래스 '$serviceClassName'는 테스트 스텁 생성을 위해 @Service 어노테이션이 선언되어야 합니다.")
        }

        val packageName = serviceClass.java.packageName
        val simpleClassName = serviceClass.simpleName ?: throw IllegalStateException("서비스 클래스 이름이 null입니다.")
        val testClassName = "${simpleClassName}Test"
        val serviceInstanceName = simpleClassName.replaceFirstChar { it.lowercaseChar() }

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
            it.visibility == KVisibility.PUBLIC && it.name !in setOf("equals", "hashCode", "toString", "copy")
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
                isTransactional = function.findAnnotation<Transactional>() != null,
                isMutator = isMutatorByAnnotation || isMutatorByNameConvention
            )
        }

        println("StubBuilder Analyzer: '$serviceClassName' 서비스 분석 완료.")
        return StubGenerationInfo(
            packageName = packageName,
            simpleClassName = simpleClassName,
            serviceType = ClassName(packageName, simpleClassName),
            testClassName = testClassName,
            serviceInstanceName = serviceInstanceName,
            mockProperties = mockProperties,
            testMethods = testMethods
        )
    }
}
