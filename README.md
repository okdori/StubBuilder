# StubBuilder: 지루한 테스트 스텁 작성은 이제 그만\!


## 💡 개요
**StubBuilder**는 Spring Boot 서비스 계층을 위한 JUnit 5 및 MockK 기반의 테스트 스텁 코드를 자동으로 생성해 주는 Gradle 플러그인입니다. 반복적이고 시간이 많이 소요되는 테스트 코드의 초기 작성 과정을 자동화함으로써, 핵심 비즈니스 로직과 실제 테스트 검증에 더 집중할 수 있도록 도와줍니다.

우리는 테스트 코드 작성의 중요성을 익히 알고 있지만, 다음과 같은 어려움에 직면하곤 합니다:
* 반복적인 보일러플레이트 코드: 각 서비스 메서드마다 Mock 객체 선언, `@BeforeEach` 설정, `given-when-then` 구조 만들기 등은 지루하고 반복적입니다.
* 초기 설정의 부담: 새로운 서비스나 복잡한 의존성을 가진 서비스의 경우, 테스트 환경을 설정하는 데만 상당한 시간이 소요됩니다.
* 일관성 부족: 여러 개발자가 각기 다른 스타일로 테스트 코드를 작성하여 코드베이스의 일관성을 해칠 수 있습니다.

**StubBuilder는 이러한 문제들을 해결해 보려고 합니다.** 서비스 클래스의 구조와 의존성을 분석하여, JUnit 5와 MockK의 모범 사례에 기반한 테스트 스텁을 생성해 드립니다.


-----


## ⚠️ 중요 사항 및 제한 사항

  * **Kotlin 프로젝트 전용:** `StubBuilder`는 KotlinPoet 및 Kotlin 리플렉션을 사용하므로, **Kotlin 언어로 작성된 프로젝트에서만 사용 가능합니다.** Java 프로젝트는 지원하지 않습니다.
  * **Spring `@Service` 어노테이션 기반:** 현재 `StubBuilder`는 **`org.springframework.stereotype.Service` 어노테이션이 붙은 클래스만** 스텁 생성 대상으로 판단합니다. 다른 Spring 컴포넌트(예: `@Controller`, `@Repository`)나 일반 Kotlin 클래스는 현재 자동 스캔 대상이 아닙니다.
  * **생성자 주입 필수:** `StubBuilder`는 서비스 클래스의 **생성자를 리플렉션으로 분석하여 의존성을 파악**합니다. 따라서 서비스 클래스의 의존성은 반드시 **생성자 주입 방식**을 사용해야 합니다. 필드 주입(`@Autowired`)이나 세터 주입 방식은 현재 지원하지 않습니다.


-----


## 🚀 시작하기

### 1\. 프로젝트에 플러그인 추가

`StubBuilder` Gradle 플러그인을 사용하려면, 프로젝트의 `build.gradle.kts` 파일에 다음 내용을 추가합니다.

```kotlin
// build.gradle.kts (또는 루트 settings.gradle.kts 파일에 pluginManagement 블록에 추가)
plugins {
    id("com.okdori.stubbuilder") version "0.0.0"
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

### 2\. StubBuilder 태스크 설정


### 3\. 스텁 생성 실행


-----

## 📄 라이선스
이 프로젝트는 MIT 라이선스에 따라 배포됩니다.
