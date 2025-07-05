package com.okdori.stubbuilder.test

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.okdori.stubbuilder.common.annotation.DataMutator
import java.util.Optional

/**
 * packageName    : com.okdori.stubbuilder.generator
 * fileName       : SampleUserService
 * author         : okdori
 * date           : 2025. 7. 5.
 * description    :
 */
data class SampleUser(val id: Long, val name: String)

interface SampleUserRepository {
    fun findById(id: Long): Optional<SampleUser>
    fun save(user: SampleUser): SampleUser
}

@Service
class SampleUserService(
    private val sampleUserRepository: SampleUserRepository // 의존성 주입 예시
) {

    fun getUserById(id: Long): Optional<SampleUser> {
        println("Calling getUserById with id: $id")
        return sampleUserRepository.findById(id)
    }

    @Transactional
    @DataMutator
    fun createUser(name: String): SampleUser {
        println("Calling createUser with name: $name")
        val newUser = SampleUser(0, name) // ID는 임시로 0
        return sampleUserRepository.save(newUser)
    }

    fun deleteUser(id: Long): Unit {
        println("Calling deleteUser with id: $id")
        // 실제 삭제 로직
    }

    fun getActiveUserCount(): Int {
        println("Calling getActiveUserCount")
        return 100
    }

    fun getAllUserNames(): List<String> {
        println("Calling getAllUserNames")
        return listOf("Alice", "Bob")
    }
}
