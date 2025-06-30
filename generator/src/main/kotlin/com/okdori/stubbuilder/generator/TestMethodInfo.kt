package com.okdori.stubbuilder.generator

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
