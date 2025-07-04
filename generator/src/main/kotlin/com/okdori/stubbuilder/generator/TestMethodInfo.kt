package com.okdori.stubbuilder.generator

import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KParameter

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : TestMethodInfo
 * author         : okdori
 * date           : 2025. 6. 30.
 * description    :
 */

/**
 * 서비스 메서드를 분석하여 찾아낸 테스트 스텁 생성을 위한 정보
 *
 * @property functionName 메서드 이름
 * @property parameters 메서드의 매개변수 목록
 * @property returnType 메서드의 반환 타입
 * @property isTransactional @Transactional 어노테이션이 붙어 있는지 여부
 * @property isMutator 데이터를 생성, 수정, 삭제하는 메서드인지 여부
 */
data class TestMethodInfo(
    val functionName: String,
    val parameters: List<KParameter>,
    val returnType: String,
    val isTransactional: Boolean,
    val isMutator: Boolean
)

/**
 * 스텁 생성을 위한 모든 정보를 담는 데이터 클래스입니다.
 * 이 정보는 [ServiceClassAnalyzer]에 의해 추출됩니다.
 */
data class StubGenerationInfo(
    val packageName: String,
    val simpleClassName: String,
    val serviceType: ClassName, // Spring @Service 클래스의 실제 TypeName
    val testClassName: String,
    val serviceInstanceName: String,
    val mockProperties: List<MockProperty>, // 생성자 의존성 Mocking 정보
    val testMethods: List<TestMethodInfo> // 각 서비스 메서드에 대한 테스트 정보
)
