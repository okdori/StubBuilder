package com.okdori.stubbuilder.common.annotation

/**
 * packageName    : com.okdori.stubbuilder.common.annotation
 * fileName       : DataMutator
 * author         : okdori
 * date           : 2025. 6. 30.
 * description    :
 */

/**
 * 메서드가 데이터를 변경(생성, 수정, 삭제 등)하는 역할을 표시
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataMutator
