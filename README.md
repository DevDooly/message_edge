# 📱 Notification Edge (알림 엣지)

<p align="center">
  <img src="docs/images/notification_edge_preview.jpg" alt="Notification Edge App Preview" width="360" style="border-radius: 20px; box-shadow: 0 8px 30px rgba(0,0,0,0.5);" />
</p>

<p align="center">
  <a href="https://github.com/DevDooly/message_edge/releases/latest"><img src="https://img.shields.io/badge/Download-Latest%20APK-00E5FF?style=for-the-badge&logo=android&logoColor=black" alt="Download APK" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen?style=for-the-badge" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-purple?style=for-the-badge&logo=kotlin" alt="Kotlin" />
</p>

과거 삼성 갤럭시 스마트폰에서 많은 사랑을 받았던 **Notification Edge(알림 엣지 패널)** 기능을 최신 안드로이드(Android 8.0 ~ Android 14+) 및 One UI 환경에 맞춰 독립형 오버레이로 새롭게 재구현한 애플리케이션입니다.

화면 가장자리에서 제스처를 통해 최근 알림을 실시간으로 확인하고, **빠른 답장(Quick Reply)**, **과거 대화 내역 누적 조회**, **알림 삭제**, **엣지 라이팅(Edge Lighting)** 테두리 네온 효과를 100% 순정처럼 경험할 수 있습니다.

---

## 📥 APK 다운로드 및 설치

최신 빌드된 APK는 GitHub Releases 페이지에서 바로 다운로드하여 설치하실 수 있습니다. 고정 서명 키가 적용되어 있어 앱을 지울 필요 없이 덮어쓰기 업데이트가 지원됩니다:

👉 **[최신 Notification Edge APK 다운로드 (Releases)](https://github.com/DevDooly/message_edge/releases/latest)**

---

## 💡 갤럭시 기본 엣지 패널과 함께 편하게 사용하는 방법 (추천 설정)

> 📖 **[👉 상세 가이드: 갤럭시 기본 Edge & Good Lock 완벽 연동 가이드 문서 바로가기](docs/Samsung_Edge_Integration_Guide.md)**

갤럭시 스마트폰의 **기본 Edge 패널(시스템 핸들)**과 본 앱의 **알림 엣지 핸들**이 겹쳐서 불편할 경우, 아래의 추천 설정 중 하나를 선택하시면 간섭 없이 완벽한 일체감으로 사용하실 수 있습니다:

### ⭐ 추천 1: 삼성 Good Lock [One Hand Operation +] 연동 (가장 추천 / 100% 순정 일체감)

화면에 별도의 핸들 바를 띄우지 않고, **갤럭시 제스처만으로 알림 엣지 패널을 0초 만에 슬라이드 아웃**하는 가장 깔끔한 방법입니다.

1. **Good Lock 설치**: **Galaxy Store**에서 삼성 공식 **`Good Lock`** 앱 및 **`One Hand Operation +`** 모듈을 설치합니다.
2. **핸들 선택**: **One Hand Operation +**를 켜고 **오른쪽 핸들** (또는 왼쪽 핸들)을 터치합니다.
3. **제스처 지정**: 원하는 제스처(예: **`대각선 아래로`** 또는 **`대각선 아래로 길게 당기기`**)를 선택합니다.
4. **앱 실행 등록**: 제스처 동작 목록에서 **`애플리케이션 실행` ➔ `Notification Edge`**를 선택합니다.
5. **핸들 숨김 설정**: **Notification Edge** 앱 설정에서 **`핸들 바 화면 표시`**를 **OFF(끄기)**로 변경합니다.
6. **사용 방법**:
   - 평소에는 화면 옆을 **수평으로 밀면** ➔ **삼성 기본 엣지 패널** 실행
   - **대각선 아래로 밀면** ➔ **알림 엣지 패널 + 테두리 엣지 라이팅**이 즉시 슬라이드 아웃!
   - 닫을 때는 패널 화면을 **손가락으로 왼쪽 드래그(스와이프)**하거나 바깥 투명 영역을 터치하면 바로 닫힙니다.

---

### 2️⃣ 추천 2: 좌/우 분리 배치 (양손 제스처)
- **갤럭시 기본 Edge 패널**: 화면 **우측(Right)** 유지
- **본 앱(Notification Edge)**: 설정에서 핸들 위치를 **`왼쪽 (Left)`**으로 설정
- **효과**: 오른쪽을 당기면 기본 앱/도구 패널이 열리고, 왼쪽을 당기면 알림 엣지가 열려 완벽히 분리됩니다.

### 3️⃣ 추천 3: 상/하 높이 분리 배치 (동일 방향 사용 시)
- **갤럭시 기본 Edge 패널**: 시스템 설정에서 핸들 위치를 `우측 상단 (위쪽 20~30%)`으로 이동
- **본 앱(Notification Edge)**: 앱 설정에서 `상하 위치 조절 슬라이더`를 `70%~80% (우측 하단)`으로 설정
- **효과**: 엄지손가락이 닿기 쉬운 아래쪽은 알림 엣지, 위쪽은 기본 도구 패널로 편리하게 구분됩니다.

---

## 📱 주요 기능

1. **실시간 알림 캡처 (Notification Listener)**
   - `NotificationListenerService` 기반의 실시간 푸시 알림 감지
   - 앱 아이콘, 발신자, 메시지 본문, 수신 시간 실시간 파싱
   - 카카오톡, 문자(SMS), 메신저 등의 대화형 알림(`MessagingStyle`) 메시지 내역 지원
   - 시스템 상태바에서 알림이 지워져도 패널 히스토리에 **과거 알림 최대 150개까지 누적 보관**

2. **과거 대화 내역 누적 및 확장 조회 (`이전 대화 더보기`)**
   - 동일 대화방(카카오톡/문자)에서 새 메시지가 오면 이전 대화 목록 뒤에 차곡차곡 누적(최대 50개)
   - 카드의 **`▼ 이전 대화 더보기`** 버튼을 눌러 과거 긴 대화 내역 전체를 자유롭게 확인

3. **슬라이드 아웃 엣지 패널 & 제스처 닫기**
   - 100% 완전 투명 배경으로 뒤의 앱 화면(유튜브, 웹브라우저 등)을 가리지 않는 글래스모피즘 오버레이
   - 화면 터치 후 **왼쪽 드래그(스와이프)** 또는 바깥 투명 영역 터치 시 즉시 닫기
   - 개별 알림 삭제 및 **'모두 지우기'** 지원

4. **인라인 빠른 답장 (Quick Reply)**
   - 카카오톡, 문자, 메신저 등 RemoteInput 지원 알림의 경우 패널 내에서 즉시 텍스트 답장 전송

5. **엣지 라이팅 테두리 효과 (Edge Lighting Effect)**
   - 새 알림 수신 시 화면 테두리를 따라 빛나는 부드러운 네온 그라데이션 애니메이션
   - 커스텀 색상 및 발광 지속 시간 조절 지원

6. **자유로운 커스터마이징 UI (Jetpack Compose & Material 3)**
   - 엣지 핸들 위치 (좌/우, 상하 Y축 비율 10%~90%)
   - 엣지 핸들 화면 표시 On/Off (투명 제스처 전용 모드)
   - 엣지 핸들 크기, 투명도, 테마 색상 설정
   - 햅틱 진동 피드백 On/Off
   - 전체 화면 설정 페이지 & 상단/하단 앱 버전 표기

---

## 🛠 기술 스택 및 아키텍처

- **언어**: Kotlin 2.0.21
- **UI 프레임워크**: Jetpack Compose, Material 3
- **아키텍처**: Modern Android Architecture (MVVM, Repository Pattern)
- **비동기 / 리액티브**: Kotlin Coroutines, StateFlow, SharedFlow
- **데이터 저장소**: Jetpack DataStore Preferences
- **시스템 연동**:
  - `NotificationListenerService` (실시간 알림 감지 및 빠른 답장 전송)
  - `WindowManager` + `TYPE_APPLICATION_OVERLAY` (엣지 오버레이 렌더링)
  - `ComposeView` + Custom `OverlayLifecycleOwner` (오버레이 내 Compose 생명주기 관리)
  - `ForegroundService` (Android 14+ 대응 백그라운드 상주)

---

## 🔒 필요 권한 안내

1. **다른 앱 위에 표시 (SYSTEM_ALERT_WINDOW)**: 화면 가장자리에 엣지 오버레이 패널을 띄우기 위해 필수입니다.
2. **알림 접근 허용 (BIND_NOTIFICATION_LISTENER_SERVICE)**: 수신되는 푸시 알림을 감지하여 엣지 패널에 표시하기 위해 필수입니다.
3. **배터리 사용량 최적화 중지 (선택)**: 백그라운드에서 서비스가 시스템에 의해 절전 종료되는 것을 방지합니다.
