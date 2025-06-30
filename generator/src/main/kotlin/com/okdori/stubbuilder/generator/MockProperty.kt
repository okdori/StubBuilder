package com.okdori.stubbuilder.generator

import kotlin.reflect.KClass

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : MockProperty
 * author         : okdori
 * date           : 2025. 6. 30.
 * description    :
 */

/**
 * 서비스 생성자를 분석하여 찾아낸 Mocking이 필요한 의존성 정보
 *
 * @property name Mock 객체의 이름
 * @property type Mock 객체의 타입
 */
data class MockProperty(val name: String, val type: KClass<*>)
