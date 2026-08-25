<div align="center">

# 📱 Notification Edge (알림 엣지)

**화면 가장자리 제스처로 최근 알림을 확인하고 바로 답장하는 안드로이드 알림 패널 앱**

<p align="center">
  <img src="docs/images/notification_edge_preview.jpg" alt="Notification Edge Preview" width="360" style="border-radius: 20px; box-shadow: 0 8px 30px rgba(0,0,0,0.4);" />
</p>

[![Latest Release](https://img.shields.io/github/v/release/DevDooly/message_edge?style=for-the-badge&color=00E5FF&logo=github&label=Release)](https://github.com/DevDooly/message_edge/releases/latest)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

<br/>

[📥 최신 APK 다운로드](https://github.com/DevDooly/message_edge/releases/latest) • 
[📖 갤럭시 Good Lock 연동 가이드](docs/Samsung_Edge_Integration_Guide.md) • 
[📋 해결 과제 및 미해결 이슈](docs/PENDING_ISSUES.md) • 
[🎨 앱 아이콘 후보 리스트](docs/App_Icon_Concepts.md) • 
[🐛 이슈 제보 및 기능 제안](https://github.com/DevDooly/message_edge/issues)

</div>

---

## 📌 소개

과거 삼성 갤럭시 스마트폰에서 제공되었던 **'알림 엣지'** 기능을 최신 안드로이드(Android 8.0 ~ 14+)에서 사용할 수 있도록 만든 앱입니다.

게임, 유튜브 시청, 웹 서핑 중에도 화면을 벗어나지 않고 **화면 가장자리 제스처로 최근 알림을 바로 확인**하고, **카카오톡이나 문자 앱으로 이동하지 않고도 엣지 패널 안에서 바로 답장**을 보낼 수 있습니다.

---

## ⚙️ 주요 기능

- **빠른 답장 및 대화 내역 유지**: 알림에서 바로 답장을 보낼 수 있으며, 내가 보낸 답장도 목록에 추가되어 메신저 앱을 열지 않고 대화를 이어갈 수 있습니다.
- **키보드 전송 버튼 & 자동 스크롤**: 답장 버튼을 누르면 가상 키보드가 뜨고, 키보드 바로 위에 전송 버튼이 위치하여 한 손으로 입력하기 편리합니다. 알림 카드도 키보드에 가려지지 않게 화면 상단으로 자동 이동합니다.
- **상태바 가림 방지 & 뒤로가기 닫기**: 화면 상단의 배터리, 시계, 시스템 알림 아이콘을 가리지 않으며, 뒤로가기 버튼이나 제스처로 키보드 및 패널을 단계별로 닫을 수 있습니다.
- **채팅방 이동 시 알림 자동 삭제**: 알림을 눌러 해당 앱(카카오톡 등)으로 이동하면 목록에서 해당 알림 카드가 자동으로 정리됩니다 (설정에서 켜고 끌 수 있음).
- **화면 테두리 엣지 라이팅**: 알림이 오면 화면 테두리에 불빛 효과가 켜집니다 (색상, 시간, 모서리 둥글기 0~50dp 조절 가능).
- **핸들 및 패널 크기 조절**:
  - 패널 가로 너비: 220dp ~ 360dp (5dp 단위 조절)
  - 핸들 두께: 4dp ~ 30dp
  - 핸들 높이: 50dp ~ 200dp
  - 핸들 위치(좌/우, 상하 위치), 투명도, 색상 조절 지원
  - 핸들 숨김 모드: 핸들을 화면에서 완전히 숨기고 Good Lock 제스처로만 열기 가능
- **인앱 원클릭 자동 업데이트**: 앱 설정 화면에서 최신 버전 확인 및 다운로드/설치를 바로 진행할 수 있습니다.

---

## 📥 설치 방법

1. 👉 **[최신 APK 다운로드](https://github.com/DevDooly/message_edge/releases/latest)**에서 APK 파일을 다운로드하여 설치합니다.
2. 앱 실행 후 안내에 따라 **'다른 앱 위에 표시'** 및 **'알림 접근'** 권한을 허용합니다.

> **설치 차단 오류가 뜰 때**:
> - 갤럭시 One UI 6+: 스마트폰 `설정` ➔ `보안 및 개인정보 보호` ➔ `보안 위험 자동 차단` ➔ **`사용 안 함`**
> - Play 프로텍트 경고: `세부정보 더보기` ➔ **`무시하고 설치`** 선택

---

## 💡 갤럭시 Good Lock (One Hand Operation +) 제스처 연동

화면에 핸들을 띄우지 않고 갤럭시 기본 제스처로 열고 싶을 때 추천하는 설정입니다.

1. Galaxy Store 또는 Play Store에서 **`Good Lock`** 및 **`One Hand Operation +`**을 설치합니다.
2. `One Hand Operation +` 실행 ➔ `오른쪽 핸들` (또는 왼쪽) ➔ 원하는 제스처(예: `대각선 아래로 당기기`)를 선택합니다.
3. 동작 목록에서 `애플리케이션 실행` ➔ **`Notification Edge`**를 지정합니다.
4. Notification Edge 앱 설정에서 **`핸들 바 화면 표시`를 OFF**로 끕니다.
5. 이제 수평 스와이프는 갤럭시 기본 엣지, 대각선 아래 스와이프는 알림 엣지로 분리해서 쓸 수 있습니다.

---

## ❓ 상단 '다른 앱 위에 표시됨' 시스템 알림 끄는 방법

안드로이드 OS 자체 안내 알림으로 상단 바에 알림이 계속 떠 있는 경우:

- **상단 바에서 끄기**: 상단 바의 `다른 앱 위에 표시됨` 알림을 **길게 꾹 누름** ➔ **`[알림 끄기]`** ➔ **`적용`**
- **시스템 설정에서 끄기**: 스마트폰 `설정` ➔ `애플리케이션` ➔ `시스템 앱 표시 켜기` ➔ `Android 시스템` ➔ `알림` ➔ `알림 카테고리` ➔ `다른 앱 위에 표시되는 앱` OFF

---

## 🔒 개인정보 보호

- 알림 및 메시지 내용은 스마트폰 기기 내부 메모리에서만 처리되며, **외부 서버로 전송되지 않습니다.**
- 인터넷 권한은 **앱 내 최신 버전 업데이트 확인 및 APK 다운로드 시에만 사용**됩니다.
- 광고나 유료 결제가 없는 비상업적 오픈소스 앱입니다.

---

## 📜 라이선스

본 프로젝트는 **[Apache License 2.0](LICENSE)**에 따라 자유롭게 사용 및 수정, 재배포할 수 있습니다.
