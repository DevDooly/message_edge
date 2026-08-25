---
name: android-overlay-expert
description: >-
  안드로이드 WindowManager 오버레이(SYSTEM_ALERT_WINDOW), 투명 액티비티 호스팅, 포그라운드 서비스,
  NotificationListenerService 및 OS 버전별(Android 8.0~15) 포커스/뒤로가기 제어 전문 스킬.
  오버레이 UI, 시스템 서비스, 백그라운드 수명주기, 배터리 최적화 관련 작업 시 활용.
---

# Android Overlay & System Service Expert Skill

Notification Edge 프로젝트의 핵심인 안드로이드 오버레이 윈도우, 투명 호스트 액티비티, 시스템 알림 리스너, 포그라운드 서비스의 아키텍처 원칙과 트러블슈팅 가이드를 제공합니다.

---

## 🏗️ 1. 핵심 아키텍처 원칙

### 1) 오버레이 윈도우 vs 투명 액티비티 분리 원칙
* **핸들 및 라이팅(Edge Handle & Lighting)**:
  - `EdgeOverlayService`의 `WindowManager.addView()`로 상시 화면 가장자리에 표시.
  - 플래그: `FLAG_NOT_FOCUSABLE` 또는 `FLAG_NOT_TOUCH_MODAL`을 사용하여 다른 앱의 터치를 방해하지 않음.
* **엣지 알림 패널(Edge Panel Content)**:
  - `EdgePanelActivity` (완전 투명 무애니메이션 호스트 액티비티)로 실행.
  - **이유**: 안드로이드 OS(특히 삼성 One UI)에서 네비게이션 뒤로가기 버튼/제스처를 100% 가로채어 닫기 위해서는 OS 정식 포그라운드 액티비티 수명주기가 필수적임.

### 2) 투명 테마(`Theme.NotificationEdge.TranslucentPanel`) 구성
```xml
<style name="Theme.NotificationEdge.TranslucentPanel" parent="Theme.NotificationEdge">
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowContentOverlay">@null</item>
    <item name="android:windowNoTitle">true</item>
    <item name="android:windowIsFloating">false</item>
    <item name="android:backgroundDimEnabled">false</item>
    <item name="android:windowAnimationStyle">@null</item>
    <item name="android:windowDisablePreview">true</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

### 3) 0ms 트랜지션 및 뒤로가기 핸들러 표준
* `EdgePanelActivity` 종료 시 항상 `overridePendingTransition(0, 0)`를 호출하여 시스템 애니메이션 없이 즉시 닫힘.
* `onBackPressedDispatcher.addCallback`을 통해 안드로이드 8부터 14/15까지 하단 소프트키 및 화면 가장자리 스와이프 제스처를 100% 처리.

---

## 🔔 2. NotificationListenerService 수명주기 관리

1. **미디어 컨트롤 알림 필터링**:
   - `CATEGORY_TRANSPORT` 및 `EXTRA_MEDIA_SESSION` 알림은 엣지 알림 목록에서 자동 제외하여 미디어 재생 간섭 방지.
2. **발신자/본문 중복 정제**:
   - `NotificationTextCleaner`를 통해 `^[가-힣a-zA-Z0-9_\.\s]{1,20}[:：\-]\s*(.+)` 정규식으로 본문 맨 앞의 중복 발신자 접두어 무조건 잘라내기.
3. **서비스 생존 보장**:
   - `BOOT_COMPLETED` 리시버 및 `NotificationEdgeApp`에서 서비스 바인딩 상태 상시 체크.

---

## ⚠️ 3. 절대 금지 사항 (Anti-Patterns)
* ❌ `EdgeOverlayService`에서 뒤로가기 키를 받기 위해 오버레이 윈도우에 `FLAG_NOT_FOCUSABLE`을 무작정 끄고 켜는 행위 (배경 앱 터치 마비 유발).
* ❌ `MainActivity`에서 무거운 UI를 직접 렌더링하는 행위 (`MainActivity`는 0ms NoDisplay 트램펄린으로 유지).
