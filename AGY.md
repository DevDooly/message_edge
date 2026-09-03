# ⚡ Notification Edge 단일 컨텍스트 기준 문서 (AGY.md)

> **Antigravity AI 개발 에이전트를 위한 단일 진실 공급원(Single Source of Truth)**  
> 본 문서는 Notification Edge 프로젝트에서 토큰 소모를 최소화하고 개발 정확도를 극대화하기 위해 아키텍처, 기술 제약, 디렉터리 역할, 코딩 컨벤션, 검증 파이프라인을 집약한 기준 문서입니다.

---

## 📌 1. 필수 사용자 규칙 (User Rules - Absolute)

1. **언어 표준**: README, `AGY.md` 등 모든 마크다운(`*.md`) 문서와 Git 커밋 메시지, 태그 릴리즈 노트는 **반드시 100% 한글로 작성**한다.
2. **원격 자동 푸시**: 기능 구현 및 코드 수정 후 빌드 검증이 완료되면, 사용자에게 추가 확인을 묻지 않고 **로컬 커밋 및 원격 저장소(`git push origin main` 및 태그)까지 자동 푸시**한다.
3. **미디어 PiP 보호 원칙**: 유튜브 등 미디어 재생 중 알림 패널이나 설정창 실행 시 백그라운드 영상이 PiP(Picture-in-Picture) 팝업으로 강제 전환되지 않도록 무화면 트램펄린 또는 완전 투명 호스트 액티비티를 강제한다.

---

## 🏗️ 2. Core Architecture & Data Flow

### 1) 단방향 리액티브 데이터 흐름 (UDF)
```
[System Notification] 
       │
       ▼
[NotificationListener] (NotificationListenerService)
       │  ├─ 1. 패키지 발견 기록 ──▶ SettingsRepository.addDiscoveredPackage()
       │  ├─ 2. 앱/키워드 필터링 ──▶ (excludedPackages / blockedKeywords 검사)
       │  └─ 3. 정제 및 파싱    ──▶ NotificationTextCleaner / MessengerNotificationParser
       ▼
[NotificationRepository] (Pure Kotlin Singleton)
       │  ├─ StateFlow<List<EdgeNotification>> (In-Memory Hot Stream)
       │  └─ SharedFlow<EdgeNotification> (신규 알림 이벤트 방출)
       ▼
[EdgeOverlayService] / [EdgePanelActivity] (Jetpack Compose UI)
       │  ├─ EdgeOverlayService : 플로팅 핸들 Window & 엣지 라이팅 ComposeView
       │  └─ EdgePanelActivity  : 100% OS 뒤로가기 보장 완전 투명 호스트 패널
```

### 2) 상태 영속화 (Persistence)
* **`SettingsRepository`**: Jetpack DataStore Preferences 기반.
* **`AppSettings`**: 불변(Immutable) Data Class 모델. `settingsFlow: Flow<AppSettings>`로 전체 앱에 상태 브로드캐스팅.

### 3) 엣지 패널 윈도우 & 생명주기 관리
* **`EdgePanelActivity`**: `Theme.NotificationEdge.TranslucentPanel` 테마 기반의 투명 액티비티.
  - OS 네비게이션 뒤로가기(하단 바 및 화면 제스처) 콜백을 100% 수신 및 보장.
  - 패널 활성화 상태를 싱글톤(`isInstanceActive`)으로 추적하여 핸들 재터치 시 즉시 토글 닫기(`closeActiveInstance()`) 수행.
* **`MainActivity`**: `Theme.NotificationEdge.TranslucentLauncher` 무화면 트램펄린. 0ms만에 패널 또는 설정을 열고 즉시 `finish()`.

---

## 🛠️ 3. Tech Stack & Constraints

### 1) 기술 스택 명세
| 구분 | 기술 / 라이브러리 | 버전 / 비고 |
| :--- | :--- | :--- |
| **Language** | Kotlin | 2.0 (JVM Target 17, Java 17 호환) |
| **Target OS** | Android SDK | MinSdk 26 (Android 8.0) ~ Compile/TargetSdk 34 (Android 14) |
| **UI Framework** | Jetpack Compose | BOM 2024.09.00, Material 3, Edge-to-Edge |
| **Storage** | Jetpack DataStore Preferences | 1.1.1 (비동기 코루틴 연동) |
| **Concurrency** | Kotlin Coroutines & Flow | StateFlow, SharedFlow, SupervisorJob |
| **Testing** | JUnit4, MockK, Robolectric, Turbine | 로컬 JVM 단위 테스트 지향 |

### 2) 기술적 제약 및 금지 사항 (Strict Constraints)
* ❌ **무거운 DI 프레임워크 도입 금지**: Hilt, Dagger 사용 금지. 빠른 콜드 스타트(0ms 오버레이)를 위해 싱글톤 및 직접 주입 유지.
* ❌ **무거운 네트워킹 라이브러리 도입 금지**: Retrofit, Ktor, OkHttp 배제. GitHub Releases 체크는 `HttpURLConnection` 및 표준 코루틴 스트림 사용.
* ❌ **XML 레이아웃 추가 금지**: 모든 화면, 오버레이, 라이팅 효과는 100% Jetpack Compose로 작성.
* ❌ **표준 불투명 액티비티 런칭 금지**: 엣지 패널 런칭 시 애니메이션이나 불투명 윈도우 전환을 유발하는 액티비티 사용 금지 (PiP 강제 전환 방지).

---

## 📂 4. Directory & Layer Roles

```
app/src/main/java/com/devdooly/notificationedge/
├── MainActivity.kt                 # [Root] 런처/Good Lock 무화면 트램펄린 (0ms 실행)
├── NotificationEdgeApp.kt          # [Root] Application 클래스
├── data/
│   ├── model/                      # 순수 불변 데이터 모델 (EdgeNotification, AppSettings 등)
│   ├── repository/                 # 인메모리 알림 저장소(NotificationRepo), DataStore 영속화(SettingsRepo)
│   └── updater/                    # GitHub Releases 기반 인앱 자동 업데이트 매니저
├── service/
│   ├── EdgeOverlayService.kt       # 포그라운드 서비스 (플로팅 핸들 오버레이 및 엣지 라이팅 렌더링)
│   ├── NotificationListener.kt     # 시스템 알림 수신, 필터링, 수신 앱 자동 기록
│   ├── OpenPanelReceiver.kt        # Good Lock / Tasker 브로드캐스트 인텐트 수신기
│   └── BootReceiver.kt             # 기기 부팅 완료 시 서비스 자동 시작 수신기
├── ui/
│   ├── overlay/                    # 엣지 패널 투명 액티비티, 패널 Compose UI, 엣지 라이팅 이펙트
│   ├── settings/                   # 설정 화면 호스트 액티비티 및 One UI 스타일 Compose 컴포저블
│   ├── theme/                      # Compose 색상, 서체, 동적 폰트, 다크 테마 정의
│   └── OpenPanelActivity.kt        # 숏컷/제스처 전용 무화면 토글 트리거 액티비티
└── util/
    ├── NotificationTextCleaner.kt  # 알림 본문 중복 발신자/접두어 정제 헬퍼
    ├── MessengerNotificationParser.kt # 메신저(카카오톡, 인스타 등) 번들 데이터 분석 파서
    ├── MediaControlHelper.kt       # 유튜브 영상 일시 정지(PiP 방지) 헬퍼
    ├── ActivityUtils.kt            # 무애니메이션 즉시 전환 윈도우 헬퍼
    └── OverlayLifecycleOwner.kt    # 서비스 윈도우 오버레이용 Compose LifecycleOwner
```

### 📌 계층별 책임 경계 규칙
* `ui` 계층은 시스템 하드웨어나 서비스를 직접 조작하지 않고, 반드시 `data/repository` 또는 `util` 헬퍼를 경유한다.
* `service` 계층은 UI 상태를 직접 참조하지 않으며, `NotificationRepository`와 `SettingsRepository`의 Flow를 구독하여 동작한다.

---

## 🎯 5. Coding Conventions & Anti-patterns

### 1) 네이밍 및 코딩 컨벤션
* 클래스/컴포저블: `UpperCamelCase` (예: `EdgePanelContent`, `SettingsRepository`)
* 함수/변수: `lowerCamelCase` (예: `addOrUpdateNotification`, `isServiceEnabled`)
* 상수: `SCREAMING_SNAKE_CASE` (예: `ACTION_OPEN_PANEL`, `DISCOVERED_APP_PACKAGES`)
* 모든 데이터 모델은 불변 `data class`로 정의하며, 상태 변경 시 `.copy()` 사용.

### 2) 절대 하지 말아야 할 안티패턴 (Strict Anti-patterns)
1. ❌ **`GlobalScope` 사용 금지**: 서비스는 `serviceScope` (`SupervisorJob() + Dispatchers.Main`), UI는 `lifecycleScope` 또는 `rememberCoroutineScope()`를 사용한다.
2. ❌ **Compose 내부 무기억 연산 금지**: 재계산이 필요한 객체나 람다는 반드시 `remember` 또는 `rememberUpdatedState`로 감싸 불필요한 Recomposition을 방지한다.
3. ❌ **무음 예외 처리(Silent Catch) 금지**: `try-catch`에서 예외를 빈 블록으로 삼키지 말고 `e.printStackTrace()`를 남기거나 적절한 Fallback 상태를 반환한다.
4. ❌ **화면 밖 터치 영역 차단 금지**: 플로팅 핸들이나 오버레이 창은 반드시 `FLAG_NOT_FOCUSABLE`을 적용하여 핸들 외 영역의 터치를 시스템으로 정상 투과시킨다.

---

## ⚡ 6. Verification & Commands (Release Pipeline)

### 1) 로컬 검증 명령어
```bash
# 로컬 JVM 단위 테스트 실행
./gradlew testDebugUnitTest

# 릴리즈 빌드 무결성 검증
./gradlew compileDebugKotlin assembleRelease
```

### 2) 버전 판올림 시 동기화 체크리스트 (4곳 필수 동기화)
1. `app/build.gradle.kts`: `versionCode`, `versionName`
2. `app/src/main/java/com/devdooly/notificationedge/ui/settings/SettingsScreen.kt`:
   - TopAppBar 버전 뱃지 (예: `v1.3.7`)
   - `AppUpdateCard(currentVersionName = "1.3.7")`
   - `AppInfoCard` (예: `버전 1.3.7 (Build 137) | Target Android 14`)
3. `docs/DEVELOPMENT_REFERENCE.md`: 최신 릴리즈 내역 및 체크리스트 갱신
4. `AGY.md`: 현재 기준 버전 명시

### 3) 표준 배포 및 원격 자동 푸시 명령어
```bash
git add .
git commit -m "타입: 한글 커밋 메시지(vX.X.X)"
git tag -a vX.X.X -m "Release vX.X.X: 상세 설명"
git push origin main
git push origin vX.X.X
```
*(GitHub Actions가 태그 푸시를 감지하여 릴리즈 APK 생성 및 GitHub Release 배포를 자동 완결)*
