---
name: android-test-automation
description: >-
  안드로이드 단위/회귀 테스트 자동화, Compose UI 테스트, DataStore Repository 테스트,
  Robolectric/MockK 모킹 및 CI 테스트 파이프라인 무결성 검증 전문 스킬.
  코드 변경 후 회귀 방지(Regression Defense) 및 테스트 작성 시 활용.
---

# Android Test Automation & Regression Defense Skill

Notification Edge 프로젝트의 신규 기능 추가 및 리팩토링 시, 기존 핵심 기능이 망가지지 않도록 자동 검증하는 테스트 표준 및 작성 가이드입니다.

---

## 🧪 1. 핵심 테스트 스위트 구조

```
app/src/test/java/com/devdooly/notificationedge/
├── data/
│   ├── model/
│   │   ├── AppSettingsTest.kt          # 앱 설정 기본값 및 직렬화 검증
│   │   └── EdgeNotificationTest.kt    # 알림 데이터 모델 검증
│   └── repository/
│       └── SettingsRepositoryTest.kt  # DataStore 및 SharedPreferences 동기화 검증
├── update/
│   └── AppUpdateManagerTest.kt        # GitHub Releases 인앱 업데이트 파서 및 검증
└── util/
    └── NotificationTextCleanerTest.kt # 발신자/전화번호 중복 접두어 무조건 잘라내기 정규식 검증
```

---

## 🛡️ 2. 필수 회귀 검증 영역 (Critical Test Areas)

### 1) 알림 텍스트 정제 (`NotificationTextCleanerTest`)
* 모든 형태의 발신자 접두어(`홍길동: 안녕하세요`, `[홍길동] 안녕하세요`, `010-1234-5678: 안녕하세요`) 정제 검증.
* 일반 특수문자나 이모지가 포함된 본문이 손상되지 않는지 검증.

### 2) 설정 동기화 (`SettingsRepositoryTest`)
* `isLaunchDirectToPanelSync()`가 앱 시작 시 동기식으로 즉시 반환되는지 검증.
* 코루틴 업데이트 시 비동기 Flow와 SharedPreferences가 동시 반영되는지 검증.

### 3) 인앱 업데이트 파서 (`AppUpdateManagerTest`)
* Semantic Versioning 비교(`1.3.10` < `1.4.0`) 로직 검증.
* GitHub Release JSON 응답에서 APK 다운로드 URL 파싱 검증.

---

## 🚀 3. 테스트 실행 명령어

```bash
# 로컬 단위 테스트 고속 실행
./gradlew testDebugUnitTest

# 전체 테스트 및 릴리즈 컴파일 통합 검증
./gradlew testDebugUnitTest assembleRelease
```
