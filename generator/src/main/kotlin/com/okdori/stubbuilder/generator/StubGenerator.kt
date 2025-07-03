package com.okdori.stubbuilder.generator

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : StubGenerator
 * author         : okdori
 * date           : 2025. 7. 3.
 * description    :
 */
interface StubGenerator {

    /**
     * 주어진 [stubInfo]를 기반으로 스텁 코드 문자열을 생성
     *
     * @param stubInfo 스텁 생성을 위한 정보
     * @return 생성된 스텁 코드 문자열
     */
    fun generate(stubInfo: StubInfo): String
}
