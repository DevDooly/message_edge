# Notification Edge (알림 엣지)

삼성 갤럭시 스마트폰에서 지원되던 "Notification Edge(알림 엣지 패널)" 기능을 최신 안드로이드(Android 8.0 ~ Android 14+) 및 One UI 환경에 맞춰 독립형으로 재구현한 애플리케이션입니다.

화면 가장자리에 상주하는 가느다란 **엣지 핸들(Edge Handle)**을 스와이프하거나 탭하여 최근 알림을 실시간으로 확인하고, 빠른 답장(Quick Reply), 알림 삭제, 앱 바로 실행, **엣지 라이팅(Edge Lighting)** 테두리 반짝임 효과를 제공합니다.

---

## 📱 주요 기능

1. **실시간 알림 캡처 (Notification Listener)**
   - `NotificationListenerService` 기반의 실시간 푸시 알림 수신
   - 앱 아이콘, 보낸 사람, 메시지 내용, 수신 시간 실시간 파싱
   - 진행 중인 알림(Ongoing) 필터링 및 앱별 알림 제외(Blacklist) 지원

2. **슬라이드 아웃 엣지 패널 (Floating Edge Overlay)**
   - 화면 좌측 또는 우측에 상주하는 반투명 엣지 트리거 바
   - 제스처(스와이프/탭) 시 부드러운 글래스모피즘(Glassmorphism) 슬라이드 패널 팝업
   - 개별 알림 스와이프 삭제 및 '모두 지우기' 지원
   - 알림 카드 탭 시 해당 앱으로 즉시 이동

3. **인라인 빠른 답장 (Quick Reply)**
   - 카카오톡, 문자(SMS), 메신저 등 RemoteInput 지원 알림의 경우 패널 내에서 즉시 텍스트 입력 및 전송

4. **엣지 라이팅 효과 (Edge Lighting Effect)**
   - 새 알림 수신 시 화면 테두리를 따라 빛나는 그라데이션 테두리 애니메이션
   - 커스텀 색상 팔레트 및 애니메이션 지속 시간 설정 지원

5. **자유로운 커스터마이징 UI (Jetpack Compose & Material 3)**
   - 엣지 핸들 위치 (좌/우, 상하 Y축 비율 10%~90%)
   - 엣지 핸들 크기 (높이/너비), 투명도, 테마 색상 설정
   - 햅틱 진동 피드백 On/Off
   - 권한 설정 가이드 (오버레이, 알림 접근, 배터리 최적화 예외)

---

## 🛠 기술 스택 및 아키텍처

- **언어**: Kotlin 2.0.21
- **UI 프레임워크**: Jetpack Compose, Material 3
- **아키텍처**: Modern Android Architecture (MVVM, Clean Architecture)
- **비동기 / 리액티브**: Kotlin Coroutines, StateFlow, SharedFlow
- **데이터 저장소**: Jetpack DataStore Preferences
- **시스템 연동**:
  - `NotificationListenerService` (알림 감지 및 액션 전달)
  - `WindowManager` + `SYSTEM_ALERT_WINDOW` (엣지 오버레이 렌더링)
  - `ComposeView` + Custom `OverlayLifecycleOwner` (오버레이 내 Compose UI 호스팅)
  - `ForegroundService` (Android 14+ 대응 백그라운드 상주)

---

## 📂 프로젝트 구조

```
notification-edge/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/devdooly/notificationedge/
│   │   │   ├── NotificationEdgeApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   ├── AppSettings.kt
│   │   │   │   │   └── EdgeNotification.kt
│   │   │   │   └── repository/
│   │   │   │       ├── NotificationRepository.kt
│   │   │   │       └── SettingsRepository.kt
│   │   │   ├── service/
│   │   │   │   ├── BootReceiver.kt
│   │   │   │   ├── EdgeOverlayService.kt
│   │   │   │   └── NotificationListener.kt
│   │   │   ├── ui/
│   │   │   │   ├── overlay/
│   │   │   │   │   ├── EdgeLightingEffect.kt
│   │   │   │   │   └── EdgePanelContent.kt
│   │   │   │   ├── settings/
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   │   └── util/
│   │   │       └── OverlayLifecycleOwner.kt
│   │   └── res/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 빌드 및 실행 방법

### 요구사항
- JDK 17 이상
- Android SDK 34 (Android 14)
- Android 디바이스 또는 에뮬레이터 (API 26 / Android 8.0 이상)

### 빌드 명령어
```bash
cd notification-edge
./gradlew assembleDebug
```

APK 파일은 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

---

## 🔒 필요 권한 안내

1. **다른 앱 위에 표시 (SYSTEM_ALERT_WINDOW)**: 화면 가장자리에 엣지 핸들 및 슬라이드 패널을 띄우기 위해 필수입니다.
2. **알림 접근 허용 (BIND_NOTIFICATION_LISTENER_SERVICE)**: 수신되는 푸시 알림을 감지하여 엣지 패널에 표시하기 위해 필수입니다.
3. **배터리 사용량 최적화 중지 (선택)**: 시스템에 의해 백그라운드 서비스가 강제 종료되는 것을 방지합니다.
