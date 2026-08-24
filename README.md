# 📱 Notification Edge (알림 엣지)

<p align="center">
  <img src="docs/images/notification_edge_preview.jpg" alt="Notification Edge App Preview" width="360" style="border-radius: 20px; box-shadow: 0 8px 30px rgba(0,0,0,0.5);" />
</p>

<p align="center">
  <a href="https://github.com/DevDooly/message_edge/releases/latest"><img src="https://img.shields.io/badge/Download-Latest%20APK-00E5FF?style=for-the-badge&logo=android&logoColor=black" alt="Download APK" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen?style=for-the-badge" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-purple?style=for-the-badge&logo=kotlin" alt="Kotlin" />
</p>

삼성 갤럭시 스마트폰에서 지원되던 **"Notification Edge(알림 엣지 패널)"** 기능을 최신 안드로이드(Android 8.0 ~ Android 14+) 및 One UI 환경에 맞춰 독립형으로 재구현한 애플리케이션입니다.

화면 가장자리에 상주하는 가느다란 **엣지 핸들(Edge Handle)**을 스와이프하거나 탭하여 최근 알림을 실시간으로 확인하고, 빠른 답장(Quick Reply), 알림 삭제, 앱 바로 실행, **엣지 라이팅(Edge Lighting)** 테두리 반짝임 효과를 제공합니다.

---

## 📥 APK 다운로드 바로가기

최신 빌드된 APK는 GitHub Releases 페이지에서 언제든 바로 다운로드하여 설치하실 수 있습니다:

👉 **[최신 Notification Edge APK 다운로드 (Releases)](https://github.com/DevDooly/message_edge/releases/latest)**

---

## 💡 갤럭시 기본 엣지 패널과 함께 편하게 사용하는 방법 (추천 설정)

> 📖 **[👉 상세 가이드: 갤럭시 기본 Edge & Good Lock 완벽 연동 가이드 문서 보기](docs/Samsung_Edge_Integration_Guide.md)**

갤럭시 스마트폰의 기본 **Edge 패널(시스템 핸들)**과 본 앱의 **알림 엣지 핸들**이 겹쳐서 화면 터치가 불편할 경우, 아래 방법 중 편한 방식을 선택하여 설정하시면 간섭 없이 편리하게 사용하실 수 있습니다:

### 1️⃣ 추천 1: 삼성 Good Lock [One Hand Operation +] 연동 (⭐ 가장 추천 - 100% 순정 일체감)
- **Good Lock (OHO+)**에서 제스처(예: 대각선 아래로 스와이프)에 **Notification Edge 실행** 지정
- 본 앱 설정에서 **`핸들 바 화면 표시`**를 **OFF(끄기)**로 설정
- **효과**: 화면에 불필요한 핸들 바가 전혀 안 뜨며, 제스처 하나로 알림 엣지가 즉시 열립니다!

### 2️⃣ 추천 2: 반대편 배치 (좌/우 분리)
- **갤럭시 기본 Edge 패널**: 오른쪽(Right)에 유지
- **본 앱(Notification Edge)**: 설정에서 **`왼쪽 (Left)`** 선택
- **효과**: 오른쪽을 당기면 갤럭시 기본 도구/앱 패널이 열리고, 왼쪽을 당기면 알림 엣지가 열려 완벽히 분리됩니다.

### 3️⃣ 추천 3: 상/하 높이 분리 배치 (동일 방향 사용 시)
- 오른쪽 가장자리를 함께 쓰고 싶을 경우:
  - **갤럭시 기본 Edge 패널**: 핸들 위치를 `우측 상단 (위쪽 20~30%)`으로 이동
  - **본 앱(Notification Edge)**: 설정에서 `상하 위치 조절 슬라이더`를 `70%~80% (우측 하단)`으로 설정
- **효과**: 엄지손가락이 닿기 쉬운 아래쪽은 알림 엣지로, 위쪽은 기본 도구 패널로 쾌적하게 구분하여 사용할 수 있습니다.

---

## 📱 주요 기능

1. **실시간 알림 캡처 (Notification Listener)**
   - `NotificationListenerService` 기반의 실시간 푸시 알림 수신
   - 앱 아이콘, 보낸 사람, 메시지 내용, 수신 시간 실시간 파싱
   - 카카오톡, 문자(SMS), 메신저 등의 대화형 알림(`MessagingStyle`) 메시지 내역 지원
   - 진행 중인 알림(Ongoing) 필터링 및 앱별 알림 제외(Blacklist) 지원

2. **슬라이드 아웃 엣지 패널 (Floating Edge Overlay)**
   - 화면 좌측 또는 우측에 상주하는 반투명 엣지 트리거 바
   - 제스처(스와이프/탭) 시 부드러운 글래스모피즘(Glassmorphism) 슬라이드 패널 팝업
   - 개별 알림 스와이프 삭제 및 '모두 지우기' 지원
   - 알림 카드 탭 시 해당 앱으로 즉시 이동

3. **인라인 빠른 답장 (Quick Reply)**
   - 카카오톡, 문자, 메신저 등 RemoteInput 지원 알림의 경우 패널 내에서 즉시 텍스트 입력 및 전송

4. **엣지 라이팅 효과 (Edge Lighting Effect)**
   - 새 알림 수신 시 화면 테두리를 따라 빛나는 그라데이션 테두리 애니메이션
   - 커스텀 색상 팔레트 및 애니메이션 지속 시간 설정 지원

5. **자유로운 커스터마이징 UI (Jetpack Compose & Material 3)**
   - 엣지 핸들 위치 (좌/우, 상하 Y축 비율 10%~90%)
   - 엣지 핸들 화면 표시 On/Off (투명 제스처 모드 지원)
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

## 🔒 필요 권한 안내

1. **다른 앱 위에 표시 (SYSTEM_ALERT_WINDOW)**: 화면 가장자리에 엣지 핸들 및 슬라이드 패널을 띄우기 위해 필수입니다.
2. **알림 접근 허용 (BIND_NOTIFICATION_LISTENER_SERVICE)**: 수신되는 푸시 알림을 감지하여 엣지 패널에 표시하기 위해 필수입니다.
3. **배터리 사용량 최적화 중지 (선택)**: 시스템에 의해 백그라운드 서비스가 강제 종료되는 것을 방지합니다.
