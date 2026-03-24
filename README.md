# InTime

멀티 모듈 구조:
- `intime-api`: 애드온 개발자가 참조하는 공개 API
- `intime-core`: 실제 서버에 설치하는 코어 플러그인

## 빌드
루트에서:
```bash
./gradlew build
```

## JitPack 사용 예시
애드온 쪽 `build.gradle`:
```gradle
repositories {
    mavenCentral()
    maven { url 'https://repo.papermc.io/repository/maven-public/' }
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT'
    compileOnly 'com.github.<GitHubUser>.<RepoName>:intime-api:1.0.0'
}
```

## 애드온에서 API 조회
```java
RegisteredServiceProvider<InTimeAPI> rsp =
        Bukkit.getServicesManager().getRegistration(InTimeAPI.class);

if (rsp == null) {
    return;
}

InTimeAPI api = rsp.getProvider();
```
