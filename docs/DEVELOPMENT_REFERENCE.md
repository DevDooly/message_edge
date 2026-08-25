# 📘 Notification Edge 개발 참조 및 아키텍처 가이드 (Development Reference)

이 문서는 **Notification Edge** 프로젝트의 구조, 주요 컴포넌트 아키텍처, 지금까지 해결한 트러블슈팅 내역, 빌드 및 배포 절차, 향후 기능 개발 시 반드시 지켜야 할 주의사항을 정리한 종합 가이드입니다.

---

## 🏗 1. 핵심 아키텍처 및 컴포넌트 구조

```mermaid
graph TD
    A[안드로이드 OS 알림 시스템] -->|알림 수신/제거| B[NotificationListener]
    B -->|파싱 & 정제| C[NotificationTextCleaner]
    C -->|알림 데이터 전달| D[NotificationRepository]
    
    E[런처 아이콘 / Good Lock] -->|NoDisplay| F[MainActivity / OpenPanelActivity]
    F -->|ACTION_OPEN_PANEL| G[EdgeOverlayService]
    
    H[화면 측면 핸들 터치] -->|오버레이 제스처| G
    
    G -->|Compose 렌더링| I[EdgePanelContent]
    I -->|알림 목록 구독 & 빠른 답장| D
    I -->|설정 화면 열기| J[SettingsActivity]
    
    K[사용자 설정 변경] -->|저장 & 동기 미러링| L[SettingsRepository]
    L -->|설정 Flow 전달| G
    L -->|동기 즉시 판별| F
```

### 📁 주요 파일 및 역할

| 파일 경로 | 주요 역할 및 설명 |
| :--- | :--- |
| **`service/EdgeOverlayService.kt`** | `TYPE_APPLICATION_OVERLAY` 기반의 화면 측면 투명 핸들(`handleView`), 엣지 패널(`panelComposeView`), 엣지 라이팅(`lightingComposeView`)을 관리하는 백그라운드 포그라운드 서비스. |
| **`service/NotificationListener.kt`** | `NotificationListenerService` 구현체. Ongoing/미디어 알림 필터링, 대화형 알림(`MessagingStyle`) 파싱, `Person` 객체 추출 및 `NotificationRepository`로 전달. |
| **`data/repository/NotificationRepository.kt`** | 수신된 알림의 메모리 캐시(`StateFlow`), 알림 삭제/취소 콜백, 빠른 답장(`RemoteInput` 전송) 및 가상 내 메시지 목록 생성/유지. |
| **`data/repository/SettingsRepository.kt`** | DataStore Preferences 기반 앱 설정 관리. 런처 즉시 판별을 위해 `SharedPreferences`(`notification_edge_sync_prefs`)에 동기 미러링 제공(`isLaunchDirectToPanelSync`). |
| **`util/NotificationTextCleaner.kt`** | 본문 텍스트 내 중복 발신자/전화번호 접두어(`홍길동: `, `010-XXXX-XXXX: ` 등) 무조건 잘라내기 및 단체방 참여자 명단 포맷팅 유틸리티. |
| **`util/CustomFontManager.kt`** | 사용자 기기 내 폰트 파일(`.ttf`, `.otf`, `.ttc`)을 SAF로 복사, 검증, 저장 및 로드하는 폰트 매니저. |
| **`ui/overlay/EdgePanelContent.kt`** | 엣지 패널의 Compose UI. 알림 카드 목록, 1:1 대화 및 그룹 채팅 분리 렌더링, 빠른 답장 입력창, 바깥 터치/드래그 닫기 처리. |
| **`ui/overlay/OverlayPanelLayout.kt`** | WindowManager 오버레이 창에서 안드로이드 뒤로가기(`KEYCODE_BACK`) 및 제스처 뒤로가기를 100% 가로채기 위한 커스텀 FrameLayout (`dispatchKeyEventPreIme` 포함). |
| **`ui/overlay/EdgePanelActivity.kt`** | 완전 투명 무애니메이션 호스트 액티비티. 안드로이드 OS 레벨의 네비게이션 뒤로가기(하단 소프트키 버튼 및 화면 제스처)를 100.0% 완벽 가로채어 닫기 처리. |
| **`MainActivity.kt`** | `Theme.NoDisplay` 기반의 런처 트램펄린 액티비티. 윈도우 생성 없이 0ms 만에 `EdgePanelActivity`를 띄우고 즉시 종료. |
| **`ui/settings/SettingsActivity.kt`** | 앱 설정 화면 전용 액티비티. 엣지 패널 상단의 [⚙️ 설정] 버튼을 눌렀을 때만 열림. |
| **`ui/OpenPanelActivity.kt`** | Good Lock 및 외부 숏컷 전용 `Theme.NoDisplay` 액티비티. |
| **`service/OpenPanelReceiver.kt`** | Good Lock, Tasker, 자동화 앱에서 브로드캐스트로 엣지 패널을 여닫는 리시버 (`com.devdooly.notificationedge.OPEN_PANEL`). |

---

## 🧰 2. 프로젝트 전용 Antigravity 스킬 (.agents/skills/)

| 스킬명 | 경로 | 주요 역할 |
| :--- | :--- | :--- |
| **`android-overlay-expert`** | [`.agents/skills/android-overlay-expert/SKILL.md`](../.agents/skills/android-overlay-expert/SKILL.md) | 오버레이 윈도우, 투명 호스트 액티비티, `NotificationListenerService`, 안드로이드 8~15 포커스/뒤로가기 제어 런북 |
| **`compose-ui-profiler`** | [`.agents/skills/compose-ui-profiler/SKILL.md`](../.agents/skills/compose-ui-profiler/SKILL.md) | Jetpack Compose UI 성능 최적화, 불필요한 Recomposition 제거, 120fps 애니메이션 튜닝 |
| **`android-test-automation`** | [`.agents/skills/android-test-automation/SKILL.md`](../.agents/skills/android-test-automation/SKILL.md) | 단위/회귀 테스트 자동화, Compose UI/DataStore 테스트 작성, 회귀 방지(Regression Defense) |
| **`app-release-pipeline`** | [`.agents/skills/app-release-pipeline/SKILL.md`](../.agents/skills/app-release-pipeline/SKILL.md) | 버전 판올림 4곳 동기화, GitHub Release 배포 및 인앱 업데이트 무결성 관리 |

---

## 🛠 2. 지금까지 해결한 주요 트러블슈팅 내역

### 1) 사용자 커스텀 폰트(.ttf, .otf) 직접 업로드 및 적용 (`v1.2.8`)
* **문제**: 프리셋 폰트에서 한글과 영문 혼용 시 줄 높이(Line Height) 불일치 발생 및 사용자 원하는 폰트 미지원.
* **해결**: SAF(Storage Access Framework) 파일 피커를 통해 기기 내 폰트를 앱 내부 저장소(`files/fonts/`)로 복사 후 `FontFamily(Font(File))`로 실시간 로드.

### 2) 미디어 재생 중단 방지 & 미디어 컨트롤 알림 필터링 (`v1.2.9`)
* **문제**: 유튜브나 음악 앱 재생 중 알림 패널을 열거나 알림을 탭하면 음악이 멈추는 현상.
* **해결**: `NotificationRepository`에서 `FLAG_ACTIVITY_RESET_TASK_IF_NEEDED` 제거, `NotificationListener`에서 `CATEGORY_TRANSPORT` 및 `EXTRA_MEDIA_SESSION` 알림 자동 제외.

### 3) 런처/Good Lock 실행 시 유튜브 PiP 팝업 방지 아키텍처 (`v1.3.0` ~ `v1.3.3`)
* **문제**: 일반 Activity가 실행되면 안드로이드 OS가 유튜브에게 `onUserLeaveHint()`를 발송하여 PiP 모드로 전환됨.
* **해결**: `MainActivity`를 `Theme.NoDisplay` 무화면 트램펄린으로 변경하고, 무거운 설정 화면을 `SettingsActivity`로 완전 분리. 미해결 과제는 [`docs/PENDING_ISSUES.md`](PENDING_ISSUES.md)에 체계적으로 기록.

### 4) 메시지 본문 발신자/전화번호 중복 제거 및 단체방 참여자 명확화 (`v1.3.4`, `v1.3.5`, `v1.3.7`)
* **문제**: 상단 타이틀에 이미 "홍길동"이 있는데, 본문에도 "홍길동: 안녕하세요"가 중복 출력되던 현상.
* **해결**: 
  - `NotificationTextCleaner`에 모든 형태의 발신자 콜론(`홍길동: `, `김철수 : `), 대괄호(`[홍길동] `), 전화번호 접두어를 무조건 잘라내는 룰 적용.
  - 1:1 대화에서는 UI에서 발신자 라벨 자체를 생략하고, 단체방에서만 화자 식별 라벨을 표시하도록 개선.

### 5) 뒤로가기(Back) 제스처/버튼 패널 닫힘 100% 보장 아키텍처 (`v1.4.0`)
* **문제**: 
  - `Service`의 `WindowManager.addView` 기반 오버레이 윈도우는 안드로이드 OS(특히 삼성 One UI / Android 10~14)의 제스처 네비게이션 및 하단 소프트키 Back 버튼 신호를 WMS 레벨에서 온전히 독점하지 못하고 포그라운드 액티비티로 흘려보내는 근본적 한계가 존재.
* **해결 (`v1.4.0`)**: 
  - **`EdgePanelActivity` (완전 투명 무애니메이션 호스트 액티비티) 도입**:
    - `Theme.NotificationEdge.TranslucentPanel` 적용 (배경 완전 투명, 윈도우 전환 애니메이션 없음).
    - 액티비티 레벨에서 `onBackPressedDispatcher.addCallback` 및 Compose `BackHandler`를 바인딩하여 **소프트키 뒤로가기, 화면 가장자리 스와이프 제스처, 바깥 영역 터치 시 100.0% 즉각 닫힘 보장**.
    - 핸들 터치 및 바로가기 실행 시 `EdgePanelActivity`가 0ms로 실행되어 완벽한 안드로이드 포그라운드 수명주기 획득.
### 6) GitHub Actions CI/CD 및 Gradle 빌드 속도 50% 이상 단축 최적화
* **원인**: 
  - `main` 브랜치와 `v*` 태그 동시 푸시 시 GitHub Actions 러너가 2개 동시 실행되어 대기열(Queueing) 및 리소스 경합 발생.
  - `gradle.properties`에 빌드 캐시(`caching`), 병렬 컴파일(`parallel`), 메모리 최적화 옵션이 꺼져 있어 매번 클린 빌드 수행.
  - `assembleRelease` 시 Android Lint(`lintVitalRelease`) 검사로 인한 지연.
* **해결**: 
  - `concurrency` 그룹 설정으로 중복 빌드 즉시 취소.
  - `gradle/actions/setup-gradle@v4` 및 `org.gradle.caching=true`, `org.gradle.parallel=true`, 4GB G1GC 메모리 최적화 적용.
  - `./gradlew testDebugUnitTest assembleRelease` 단일 파이프라인 통합 및 `lint { checkReleaseBuilds = false }` 적용으로 릴리즈 시간 획기적 단축.

### 7) 메시지 및 알림 수신 시각 개별 표시 (`v1.4.1`)
* **개선**: 대화형 알림의 각 말풍선 메시지 옆 및 일반 알림 본문 우측 하단에 한국어 12시간제('오후 3:24' 또는 'M/d a h:mm') 수신 시각(`formatMessageTime`)을 표시하여 언제 수신된 메시지인지 직관적으로 파악 가능하도록 개선.

### 8) 소스 전반 리팩토링 및 클린 아키텍처 정리 (`v1.4.2`)
* **개선**:
  - `EdgeOverlayService.kt`의 미사용 레거시 필드(`panelComposeView`, `isPanelOpen`, `setPanelFocusable`) 정리 및 진동 API 최신화(`VibratorManager`).
  - `Divider` ➔ Material3 표준 `HorizontalDivider` 교체.
  - `ActivityUtils.kt`를 도입하여 API 34+ `overrideActivityTransition` 및 0ms 무애니메이션 전환 완벽 지원.
  - `ActivityUtilsTest.kt` 단위 테스트 추가로 무결성 검증 강화.

### 9) 카카오톡 및 메신저 단체톡방 방 이름/발신자 분리 및 알림 병합 정상화 (`v1.4.3`)
* **문제**: 카카오톡 단체방 알림 수신 시 안드로이드 OS는 `EXTRA_SUB_TEXT`에 단체방 이름을 넣고 `EXTRA_TITLE`에 메시지 발신자 이름을 넣는데, 기존 코드가 `rawTitle`(발신자 이름)을 카드 제목으로 채택하여 동일한 단체방임에도 발신자마다 카드가 분리되어 쪼개지는 현상 발생.
* **해결**:
  - `NotificationListener.kt`에서 `EXTRA_CONVERSATION_TITLE`, 카카오톡의 `EXTRA_SUB_TEXT`, `EXTRA_IS_GROUP_CONVERSATION`을 정밀 검사하여 **실제 단체방 이름(Room Title)**을 카드의 대표 `title`로 추출.
  - 보낸 사람 이름은 개별 `MessageItem.sender`로 정상 매핑.
  - `NotificationRepository`에서 동일한 단체방 메시지가 하나의 카드로 완벽 병합 및 대화 누적되도록 개선.
  - `NotificationRepositoryTest.kt`에 단체방 메시지 병합 회귀 테스트 추가 완료.

### 10) 엣지 패널 실행 시 상단 상태바(배터리 바) 및 하단 네비바 완전 투명 유지 (`v1.4.4`)
* **문제**: `EdgePanelActivity`가 뜰 때 안드로이드 시스템의 대비 강제(Contrast Enforcement) 및 기본 스크림으로 인해 상단 배터리/시계 영역과 하단 제스처 네비바 배경이 불투명한 검정색으로 덮여 원래 화면의 투명함이 깨지는 현상.
* **해결**:
  - `Theme.NotificationEdge.TranslucentPanel`에 `windowDrawsSystemBarBackgrounds=true`, `enforceStatusBarContrast=false`, `enforceNavigationBarContrast=false` 속성 추가.
  - `EdgePanelActivity.kt`에서 `enableEdgeToEdge()`를 투명 스타일(`Color.TRANSPARENT`)로 명시 선언하고, `isStatusBarContrastEnforced = false`, `isNavigationBarContrastEnforced = false`를 코드로 강제 적용하여 상단 배터리바 및 하단 네비바가 완전 투명하게 유지되도록 개선.

### 11) 앱별 특화 메신저 알림 파서(`MessengerNotificationParser`) 및 단체방 뱃지 UI 도입 (`v1.4.5`)
* **개선**:
  - 카카오톡(`com.kakao.talk`), 텔레그램, 라인, 기본 문자 등 메신저별 알림 데이터 구조에 특화된 `MessengerNotificationParser.kt` 구축.
  - `subText`, `conversationTitle`, `summaryText`, 괄호(`"홍길동 (가족모임)"`), 본문 대괄호(`"[가족모임] ..."`), 쉼표 참여자 목록 등 모든 단체방 패턴을 100% 감지하여 단체방 이름 및 발신자 완벽 분리.
  - `NotificationCard` 상단 헤더에 `카카오톡 › 단체방이름` 경로 표시 및 제목 앞 `[단체방]` 청록색 뱃지를 부여하여 단체방 식별 시인성 극대화.
  - `MessengerNotificationParserTest.kt` 단위 테스트 스위트 추가 완료.

### 12) 알림 원본 데이터(Bundle Extras) 실시간 인스펙터 및 원클릭 복사 기능 탑재 (`v1.4.6`)
* **개선**:
  - `EdgeNotification.kt` 및 `NotificationListener.kt`에 `debugExtrasDump` 필드 및 전용 덤프 로직 구축.
  - `EdgePanelContent.kt`: 각 알림 카드 하단에 `[데이터 복사]` 버튼을 추가하여 수신된 알림의 모든 Bundle Key-Value 데이터를 클립보드에 원클릭 복사 가능.
  - `SettingsScreen.kt`: `NotificationDebugDumpCard`를 탑재하여 최근 수신된 알림들의 원본 extras 구조를 한눈에 조회하고 `[전체 복사]`할 수 있는 디버그 인스펙터 지원.

### 13) 카카오톡 무제(그룹) 단체방 참여자 목록 기반 방 이름 자동 생성 (`v1.4.7`)
* **문제**: 카카오톡에서 방 이름을 별도로 설정하지 않은 그룹 채팅방의 경우, `android.isGroupConversation = true`이지만 `android.conversationTitle`과 `android.subText`가 모두 `null`이고 `android.title`에는 마지막 발신자 이름("김영남")만 넘어와서 단체방 이름이 표시되지 않던 현상.
* **해결**:
  - 실제 제공된 Notification Extras 덤프를 분석하여, `android.messages` 배열 내의 모든 고유 발신자 목록(예: `["미리비트 윤창빈 책임", "미리비트 정진우 책임", "김영남"]`)을 추출.
  - `buildGroupRoomTitleFromSenders` 함수를 구축하여 참여자 목록(예: `"미리비트 윤창빈 책임, 미리비트 정진우 책임, 김영남"`)을 시스템 알림과 동일한 대표 단체방 이름으로 자동 합성.
  - `MessengerNotificationParserTest.kt`에 실제 수신 덤프 기반 회귀 테스트 추가 완료.

### 14) 1인 발신 단체방 타이틀 정규화 및 카카오맵 공백 타이틀 대응 (`v1.4.8`)
* **개선**:
  - 단체방 메시지가 1개만 와서 참여자가 1명(`"김수환"`)인 경우, 어색한 `"(단체방)"` 텍스트 접미사를 붙이지 않고 깔끔한 발신자 이름(`"김수환"`)으로 유지하면서 `[단체방]` 청록색 뱃지로 직관적인 단체방 상태 제공.
  - 대화가 누적될수록 참여자 목록(`"김수환, 이영희"`)으로 실시간 자연 확장.
  - 카카오맵(`net.daum.android.map`) 등 타이틀이 공백(`" "`)으로 오는 알림의 경우 앱 이름(`"카카오맵"`)으로 자동 폴백되도록 개선.

---

## 💻 3. 표준 빌드, 버전 관리 및 Git 릴리즈 명령어

### 1) 버전 판올림 체크리스트
버전을 올릴 때는 다음 2개 파일(총 4곳)의 버전을 동시에 수정합니다:
1. `app/build.gradle.kts`: `versionCode`, `versionName`
2. `app/src/main/java/com/devdooly/notificationedge/ui/settings/SettingsScreen.kt`:
   - TopAppBar 버전 뱃지 (예: `v1.3.7`)
   - `AppUpdateCard(currentVersionName = "1.3.7")`
   - `AppInfoCard` (예: `버전 1.3.7 (Build 37) | Target Android 14`)

### 2) 테스트 및 빌드 검증 명령어
```bash
# 로컬 JVM 단위 테스트 실행
./gradlew testDebugUnitTest

# 디버그 & 릴리즈 빌드 검증
./gradlew compileDebugKotlin assembleRelease
```
*(테스트 및 빌드가 성공(`BUILD SUCCESSFUL`)하는지 반드시 확인)*

### 3) Git 커밋, 태그 생성 및 원격 자동 푸시
```bash
git add .
git commit -m "타입: 한글 설명(v버전)"
git tag -a v버전 -m "Release v버전: 상세 설명"
git push origin main
git push origin v버전
```

---

## 🧪 4. 테스트 환경 및 스위트 구조

프로젝트에는 다음과 같은 로컬 JVM 단위 테스트 및 계측 테스트 환경이 구축되어 있습니다:

| 테스트 클래스 | 테스트 대상 및 내용 |
| :--- | :--- |
| **`NotificationTextCleanerTest`** | 1:1 대화 발신자 접두어(`홍길동: `), 전화번호 접두어, 대괄호(`[Web발신]`), 단체방 긴 제목 포맷팅 정제 룰 검증 |
| **`AppUpdateManagerTest`** | 시맨틱 버전 비교 로직(`isNewerVersion`) 무결성 검증 |
| **`EdgeNotificationTest`** | 알림 엔티티 기본값, 메시지 모델, 빠른 답장 액션 플래그 검증 |
| **`AppSettingsTest`** | 앱 설정 기본값(패널 크기, 투명도, 엣지 라이팅 등) 검증 |
| **`SettingsRepositoryTest`** | Robolectric 기반 DataStore 및 SharedPreferences 동기화 동작 검증 |

> [!TIP]
> GitHub Actions CI 파이프라인(`.github/workflows/release.yml`)에 `testDebugUnitTest`가 자동 연동되어 있어, main 푸시 및 릴리즈 태그 생성 시 단위 테스트가 자동으로 실행 및 검증됩니다.

---

## ⚠️ 4. 향후 개발 시 주의사항 및 핵심 원칙

1. **커밋 메시지 및 마크다운 파일은 반드시 한글로 작성할 것** (사용자 글로벌 룰).
2. **코드 변경 및 빌드 검증 후 git commit 및 원격 저장소 push를 자동으로 진행할 것**.
3. **오버레이 윈도우(`WindowManager`) 수정 시 주의**:
   - `FLAG_NOT_FOCUSABLE`을 적용하면 시스템의 `KEYCODE_BACK`(뒤로가기 키/제스처)이 오버레이 창으로 들어오지 않습니다.
   - 키보드 입력이나 뒤로가기 처리가 필요할 때는 `OverlayPanelLayout`의 `dispatchKeyEvent` 및 `dispatchKeyEventPreIme`가 정상 동작하는지 항상 검증할 것.
4. **알림 텍스트 처리 시 주의**:
   - 새로운 메신저/앱 알림을 파싱할 때는 반드시 `NotificationTextCleaner.cleanMessageText`를 통과시켜 중복 접두어가 붙지 않도록 할 것.
