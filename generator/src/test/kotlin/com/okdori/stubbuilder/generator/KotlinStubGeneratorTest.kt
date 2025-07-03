package com.okdori.stubbuilder.generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : KotlinStubGeneratorTest
 * author         : okdori
 * date           : 2025. 7. 4.
 * description    :
 */
class KotlinStubGeneratorTest {
    private val generator = KotlinStubGenerator()

    @Test
    @DisplayName("기본 데이터 클래스 스텁 생성 테스트")
    fun `generate basic data class stub`() {
        // given
        val stubInfo = StubInfo(
            packageName = "com.okdori.test",
            className = "MyDataStub"
        )

        // when
        val generatedCode = generator.generate(stubInfo)

        // then
        val expectedCode = """
            package com.okdori.test

            data class MyDataStub()
        """.trimIndent()

        assertThat(generatedCode.trim()).isEqualTo(expectedCode.trim())
    }

    @Test
    @DisplayName("기본 데이터 클래스 스텁 생성 테스트 (필드 포함)")
    fun `generate basic data class stub with fields to trigger imports`() {
        // given
        val stubInfo = StubInfo(
            packageName = "com.okdori.test",
            className = "MyDataStub",
            fields = listOf(
                FieldInfo("name", "String"),
                FieldInfo("age", "Int")
            )
        )

        // when
        val generatedCode = generator.generate(stubInfo)

        // then
        val expectedCode = """
            package com.okdori.test

            import kotlin.Int
            import kotlin.String
            
            data class MyDataStub(
              val name: String = "",
              val age: Int = 0,
            )
        """.trimIndent()

        assertThat(generatedCode.trim()).isEqualTo(expectedCode.trim())
    }

    @Test
    @DisplayName("필드를 포함하는 데이터 클래스 스텁 생성 테스트")
    fun `generate data class stub with fields`() {
        // given
        val stubInfo = StubInfo(
            packageName = "com.okdori.test",
            className = "UserStub",
            fields = listOf(
                FieldInfo("id", "Long", "123L"),
                FieldInfo("name", "String"),
                FieldInfo("isActive", "Boolean", "true"),
                FieldInfo("tags", "List<String>"),
                FieldInfo("metadata", "Map<String, Any?>"),
                FieldInfo("optionalValue", "java.util.Optional<String>")
            )
        )

        // when
        val generatedCode = generator.generate(stubInfo)

        // then
        val expectedCode = """
            package com.okdori.test

            import java.util.Optional
            import kotlin.Any
            import kotlin.Boolean
            import kotlin.Long
            import kotlin.String
            import kotlin.collections.List
            import kotlin.collections.Map
            
            data class UserStub(
              val id: Long = 123L,
              val name: String = "",
              val isActive: Boolean = true,
              val tags: List<String> = emptyList(),
              val metadata: Map<String, Any?> = emptyMap(),
              val optionalValue: Optional<String> = Optional.empty(),
            )
        """.trimIndent()

        assertThat(generatedCode.trim()).isEqualTo(expectedCode.trim())
    }

    @Test
    @DisplayName("메서드를 포함하는 데이터 클래스 스텁 생성 테스트")
    fun `generate data class stub with methods`() {
        // given
        val stubInfo = StubInfo(
            packageName = "com.okdori.test",
            className = "ServiceStub",
            methods = listOf(
                MethodInfo("doSomething", "Unit"),
                MethodInfo("calculate", "Int", listOf(ParameterInfo("a", "Int"), ParameterInfo("b", "Int"))),
                MethodInfo("getData", "String?"),
                MethodInfo("processList", "List<String>", listOf(ParameterInfo("items", "List<String>")), "return items.map { it.uppercase() }")
            )
        )

        // when
        val generatedCode = generator.generate(stubInfo)

        // then
        val expectedCode = """
            package com.okdori.test

            import kotlin.Int
            import kotlin.String
            import kotlin.collections.List
            
            data class ServiceStub() {
              fun doSomething() {
              }

              fun calculate(a: Int, b: Int): Int = 0

              fun getData(): String? = null

              fun processList(items: List<String>): List<String> = items.map { it.uppercase() }
            }
        """.trimIndent()

        assertThat(generatedCode.trim()).isEqualTo(expectedCode.trim())
    }

    @Test
    @DisplayName("복합적인 스텁 생성 테스트")
    fun `generate complex stub`() {
        // given
        val stubInfo = StubInfo(
            packageName = "com.okdori.api",
            className = "ComplexApiStub",
            fields = listOf(
                FieldInfo("userId", "Long"),
                FieldInfo("userName", "String", "\"ComplexUser\""),
                FieldInfo("permissions", "Set<String>", "setOf(\"READ\", \"WRITE\")")
            ),
            methods = listOf(
                MethodInfo("fetchUser", "com.okdori.model.User", listOf(ParameterInfo("id", "Long"))),
                MethodInfo("updateStatus", "Boolean", listOf(ParameterInfo("status", "String?")), "return status != null"),
                MethodInfo("getDetails", "Map<String, Any>", bodyTemplate = "return emptyMap()")
            )
        )

        // when
        val generatedCode = generator.generate(stubInfo)

        // then
        val expectedCode = """
            package com.okdori.api

            import com.okdori.model.User
            import kotlin.Any
            import kotlin.Boolean
            import kotlin.Long
            import kotlin.String
            import kotlin.collections.Map
            import kotlin.collections.Set
            
            data class ComplexApiStub(
              val userId: Long = 0L,
              val userName: String = "ComplexUser",
              val permissions: Set<String> = setOf("READ", "WRITE"),
            ) {
              fun fetchUser(id: Long): User = null

              fun updateStatus(status: String?): Boolean = status != null

              fun getDetails(): Map<String, Any> = emptyMap()
            }
        """.trimIndent()

        assertThat(generatedCode.trim()).isEqualTo(expectedCode.trim())
    }
}
