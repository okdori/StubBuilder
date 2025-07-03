package com.okdori.stubbuilder.generator

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : StubInfo
 * author         : okdori
 * date           : 2025. 7. 3.
 * description    :
 */

/**
 * 스텁 생성을 위한 정보를 담는 클래스
 *
 * @property packageName 생성될 스텁 클래스의 패키지 이름
 * @property className 생성될 스텁 클래스의 이름
 * @property fields 스텁 클래스에 포함될 필드들의 목록
 * @property methods 스텁 클래스에 포함될 메서드들의 목록
 */
data class StubInfo(
    val packageName: String,
    val className: String,
    val fields: List<FieldInfo> = emptyList(),
    val methods: List<MethodInfo> = emptyList()
)

/**
 * 스텁 클래스의 필드 정보를 담는 클래스
 *
 * @property name 필드 이름
 * @property type 필드 타입
 * @property defaultValue 필드의 기본값
 */
data class FieldInfo(
    val name: String,
    val type: String,
    val defaultValue: String? = null
)

/**
 * 스텁 클래스의 메서드 정보를 담는 클래스
 *
 * @property name 메서드 이름
 * @property returnType 메서드의 반환 타입
 * @property parameters 메서드의 파라미터 목록
 * @property bodyTemplate 메서드 본문 템플릿
 */
data class MethodInfo(
    val name: String,
    val returnType: String,
    val parameters: List<ParameterInfo> = emptyList(),
    val bodyTemplate: String? = null
)

/**
 * 메서드 파라미터 정보를 담는 클래스
 *
 * @property name 파라미터 이름
 * @property type 파라미터 타입
 */
data class ParameterInfo(
    val name: String,
    val type: String
)
