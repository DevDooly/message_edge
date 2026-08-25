<div align="center">

# 📱 Notification Edge (알림 엣지)

**순정 갤럭시 감성을 그대로 담은 올인원 알림 오버레이 & 실시간 빠른 답장 시스템**

<p align="center">
  <img src="docs/images/notification_edge_preview.jpg" alt="Notification Edge Preview" width="380" style="border-radius: 24px; box-shadow: 0 10px 35px rgba(0,0,0,0.6);" />
</p>

[![Latest Release](https://img.shields.io/github/v/release/DevDooly/message_edge?style=for-the-badge&color=00E5FF&logo=github&label=Release)](https://github.com/DevDooly/message_edge/releases/latest)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

<br/>

[📥 최신 APK 다운로드](https://github.com/DevDooly/message_edge/releases/latest) • 
[📖 갤럭시 Good Lock 연동 가이드](docs/Samsung_Edge_Integration_Guide.md) • 
[🐛 이슈 제보 및 기능 제안](https://github.com/DevDooly/message_edge/issues)

</div>

---

## 🌟 프로젝트 소개 (Overview)

**Notification Edge(알림 엣지)**는 과거 삼성 갤럭시 스마트폰에서 큰 사랑을 받았던 **'알림 엣지 패널'** 기능을 최신 안드로이드(Android 8.0 ~ 14+) 및 One UI 환경에 맞춰 현대적인 아키텍처와 감성적인 글래스모피즘(Glassmorphism) UI로 재탄생시킨 **오픈소스 독립형 오버레이 앱**입니다.

게임 중이거나 다른 앱을 사용하는 도중에도 화면을 벗어나지 않고 **화면 가장자리 스와이프 제스처 한 번으로 최근 알림을 실시간으로 확인**하고, **카카오톡/문자 앱으로 이동하지 않고도 엣지 패널 안에서 채팅방처럼 실시간 대화를 주고받을 수 있습니다.**

---

## ✨ 주요 핵심 기능 (Feature Highlights)

### 💬 1. 인라인 빠른 답장 & 실시간 대화 누적 (Live Chat Overlay)
- **앱 이동 없는 채팅 지속**: 알림에서 바로 답장을 보내면, 상대방 메시지 아래에 **내가 보낸 답장(`나: 메시지`)이 대화 목록에 즉시 추가**되어 실제 메신저처럼 대화가 이어집니다.
- **키보드 상단 플로팅 전송 바**: 엄지손가락이 닿기 쉬운 가상 키보드 바로 위에 답장 입력창과 전송 버튼(`[전송 ➔]`)이 제공됩니다.
- **과거 대화 내역 최대 50개 누적 보관**: 같은 대화방의 이전 메시지들을 최대 50개까지 누적 보관하며, `[이전 대화 더보기]`로 과거 내역을 한눈에 확인할 수 있습니다.

### 🪟 2. 글래스모피즘 오버레이 & 상태바 보호
- **완전 투명 배경 & 무결점 전환**: 바탕화면 터치 시 깜빡임이나 잔상 없이 즉시 사라지는 쾌적한 반응 속도를 자랑합니다.
- **최상단 상태 표시줄(Status Bar) 정보 보호**: 배터리 잔량, 현재 시간, Wi-Fi 신호, 시스템 알림 아이콘 등을 전혀 가리지 않도록 인셋 영역을 보존합니다.
- **뒤로가기(Back) 제스처 순차 닫기**: 뒤로가기 시 `가상 키보드/답장창 닫기` ➔ `엣지 패널 닫기`가 자연스럽게 동작합니다.

### 💡 3. 네온 엣지 라이팅 (Edge Lighting)
- 알림이 도착하면 화면 둘레를 따라 부드럽게 빛나는 네온 그라데이션 발광 효과를 제공합니다.
- 발광 색상(에메랄드, 퍼플, 네온 블루 등) 및 발광 지속 시간을 자유롭게 조절할 수 있습니다.

### 📐 4. 정밀한 핸들바 & 패널 커스터마이징
- **패널 가로 너비**: `220dp ~ 360dp` (5dp 단위 정밀 조절)
- **핸들바 두께/너비**: `4dp ~ 30dp` 슬라이더 지원
- **핸들바 높이 길이**: `50dp ~ 200dp`
- **위치 및 투명도**: 좌/우 사이드 전환, 상하 위치 비율(10%~90%), 투명도(0%~100%), 색상 팔레트 지원
- **핸들 숨김(제스처 전용 모드)**: 핸들을 화면에서 완전히 숨기고 Good Lock 제스처로만 깔끔하게 호출 가능

### 🔄 5. 원클릭 인앱(In-App) 자동 업데이트
- 매번 웹사이트를 찾아갈 필요 없이, 앱 설정 화면에서 **`[최신 업데이트 확인]` ➔ `[지금 다운로드 및 바로 업데이트]`** 터치 한 번으로 최신 버전이 자동 설치됩니다.
- 릴리즈 노트와 변경 사항을 앱 내부에서 바로 확인할 수 있습니다.

---

## 📥 설치 방법 (Installation)

### 1. APK 다운로드
👉 **[GitHub Releases 최신 버전 다운로드](https://github.com/DevDooly/message_edge/releases/latest)**

### ⚠️ 설치 시 '보안 위험으로 앱 차단됨' 오류 해결 방법
삼성 One UI 6.0+ (Galaxy S23/S24 등) 또는 안드로이드 보안 기능으로 인해 설치가 차단되는 경우 아래 설정을 확인해 주세요:

1. **삼성 '보안 위험 자동 차단(Auto Blocker)' 끄기** (One UI 6+ 갤럭시 기기)
   - 스마트폰 `설정` ➔ `보안 및 개인정보 보호` ➔ `보안 위험 자동 차단` ➔ **`사용 안 함(OFF)`**
2. **출처를 알 수 없는 앱 설치 허용**
   - 스마트폰 `설정` ➔ `보안 및 개인정보 보호` ➔ `출처를 알 수 없는 앱 설치` ➔ 다운로드 앱(`내 파일`, `Chrome` 등) ➔ **`허용(ON)`**
3. **Google Play 프로텍트 경고 창**
   - 팝업 창 하단의 `세부정보 더보기` ➔ **`무시하고 설치(안전하지 않음)`** 터치

---

## 💡 갤럭시 기본 엣지 & Good Lock 연동 꿀팁

> 📖 더욱 상세한 설정 안내는 **[갤럭시 기본 Edge & Good Lock 연동 가이드](docs/Samsung_Edge_Integration_Guide.md)**를 참고하세요.

### ⭐ 가장 추천하는 설정: One Hand Operation + 제스처 연동
화면에 별도 핸들을 띄우지 않고, 갤럭시 순정 제스처만으로 알림 엣지를 호출하는 가장 깔끔한 방법입니다.

1. **앱 설치**: Play Store 또는 Galaxy Store에서 `Good Lock` 및 `One Hand Operation +` 설치
2. **제스처 지정**: `One Hand Operation +` ➔ `오른쪽 핸들` ➔ `대각선 아래로 당기기` 선택
3. **동작 등록**: `애플리케이션 실행` ➔ **`Notification Edge`** 선택
4. **핸들 숨김**: Notification Edge 앱 설정에서 `핸들 바 화면 표시`를 **OFF**로 변경
5. **사용 결과**:
   - **수평 스와이프**: 삼성 기본 도구 엣지 패널 실행
   - **대각선 아래 스와이프**: Notification Edge 알림 패널 + 엣지 라이팅 즉시 호출

---

## 🛡️ 개인정보 보호 및 보안 (Privacy & Security First)

Notification Edge는 사용자의 프라이버시를 최우선으로 생각합니다:

- 🔒 **100% 온디바이스 로컬 처리**: 사용자의 알림 및 메시지 데이터는 어떠한 외부 서버로도 전송되지 않으며, 스마트폰 기기 내부 메모리에서만 안전하게 동작합니다.
- 🌐 **인터넷 권한 최소화**: 인터넷 통신은 **GitHub Releases 최신 버전 체크 및 APK 다운로드**에만 한정적으로 사용됩니다.
- 🚫 **광고 및 유료 결제 없음**: 본 앱은 비상업적 오픈소스 프로젝트로, 일체의 광고나 추적기(Tracker), 유료 결제가 포함되어 있지 않습니다.

---

## 🛠 기술 스택 (Tech Stack)

| 구분 | 기술 / 라이브러리 |
| :--- | :--- |
| **언어 (Language)** | Kotlin 2.0.21 |
| **UI 프레임워크** | Jetpack Compose, Material 3, Glassmorphism Styling |
| **아키텍처** | Modern Android Architecture (MVVM, Clean Architecture) |
| **비동기 / 반응형** | Kotlin Coroutines, StateFlow, SharedFlow |
| **데이터 영속성** | Jetpack DataStore Preferences |
| **시스템 통합** | `NotificationListenerService`, `WindowManager (TYPE_APPLICATION_OVERLAY)`, `OverlayPanelLayout (dispatchKeyEvent)`, `ForegroundService` |

---

## 📜 라이선스 (License)

본 프로젝트는 **[Apache License 2.0](LICENSE)**에 따라 배포되는 오픈소스 소프트웨어입니다. 누구나 자유롭게 소스 코드를 열람, 수정, 재배포할 수 있습니다.

```
Copyright 2026 DevDooly

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
