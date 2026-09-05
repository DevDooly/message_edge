# 📋 Slivue 해결 과제 및 미해결 이슈 목록

이 문서는 추후 집중 개선 및 기능 고도화 시 해결해야 할 미해결 이슈, 동작 상의 특이사항 및 기술적 연구 과제를 정리하는 문서입니다.

---

## 📌 이슈 #1: 유튜브/동영상 재생 중 앱 실행 시 PiP(Picture-in-Picture) 모드 자동 전환 현상

### 1. 이슈 개요 및 현상
* **증상**: 
  - 유튜브(YouTube) 영상이 **전체화면 재생 중**일 때, 홈 화면의 앱 아이콘을 터치하거나 Good Lock(One Hand Operation+) 제스처로 Slivue를 실행하면 **유튜브가 홈 버튼을 누른 것처럼 PiP(팝업 미니 플레이어) 화면으로 축소**되는 현상.
  - 유튜브 영상이 일시정지(재생 안 됨) 상태일 때는 정상 작동하나, **재생 중일 때만** 발생함.
* **영향을 받는 앱**: YouTube, YouTube Music, Netflix, Twitch 등 안드로이드 PiP 자동 진입(`setAutoEnterEnabled(true)`)이 활성화된 미디어 플레이어 앱.

---

### 2. 현재까지 진행된 분석 및 적용된 조치 내역

| 버전 | 시도한 조치 | 결과 |
| :--- | :--- | :--- |
| **v1.2.9** | 미디어 컨트롤러 백그라운드 필터링 및 포그라운드 전환 인텐트 플래그 최적화 | 알림 클릭 시 미디어 중단은 완화되었으나 제스처/런처 실행 시 PiP 전환 유지 |
| **v1.3.0** | `OpenPanelReceiver` (BroadcastReceiver) 추가 및 `OpenPanelActivity`에 `Theme.NoDisplay` 적용 | 브로드캐스트 호출은 가능하나 런처 앱 아이콘 및 Good Lock 기본 실행 시 PiP 발생 |
| **v1.3.1** | `MainActivity.onCreate()`에서 0ms 동기 판별(Fast Sync) 및 오버레이 윈도우 기본 `FLAG_NOT_FOCUSABLE` 적용 | 엣지 핸들 터치 시 포커스 강탈은 방지되었으나 런처/Good Lock 트리거 시 PiP 발생 |
| **v1.3.3** | `MainActivity`를 순수 `Theme.NoDisplay` 트램펄린으로 완전 분리 및 `SettingsActivity` 격리 | 시스템 창은 뜨지 않으나 런처/Good Lock의 `startActivity()` 호출 자체로 인해 PiP 진입 |

---

### 3. 기술적 원인 분석 (OS 레벨)

1. **안드로이드 OS의 `onUserLeaveHint()` 강제 발송 메커니즘**:
   * 안드로이드 시스템(ActivityTaskManagerService)은 사용자가 홈 화면의 아이콘을 탭하거나 Good Lock의 "애플리케이션 시작" 제스처를 실행할 때 `Context.startActivity()`를 호출합니다.
   * `startActivity()`가 발동되면, 호출 대상 액티비티가 `Theme.NoDisplay`이든 `singleInstance`이든 상관없이 **시스템은 현재 포그라운드 액티비티(유튜브)에게 `onUserLeaveHint()`를 무조건 발송**합니다.
   * 유튜브는 `onUserLeaveHint()`를 수신하는 순간 내부적으로 `enterPictureInPictureMode()`를 자동 호출하도록 설계되어 있습니다.

2. **삼성 기본 엣지 패널과의 차이점**:
   * 삼성 기본 엣지 패널(Edge Panel)은 일반 애플리케이션(`Activity`)이 아니며, **Samsung SystemUI(CocktailBarManagerService)**의 시스템 내부 서비스로 구동됩니다.
   * 따라서 제스처를 당길 때 `startActivity()` 시스템 호출이 전혀 발생하지 않고, 시스템 UI 단에서 오버레이 뷰만 직접 렌더링되므로 유튜브가 `onUserLeaveHint()`를 받지 않습니다.

---

### 4. 추후 해결을 위한 대안 및 연구 과제 (Next Steps)

1. **대안 A: AccessibilityService(접근성 서비스) 제스처 트리거 방식**
   * 접근성 권한(`AccessibilityService`)을 등록하여 화면 가장자리 제스처나 특정 단축키 입력을 액티비티 실행 없이 순수 백그라운드에서 직접 가로채어 `EdgeOverlayService`를 호출하는 방식 연구.

2. **대안 B: Good Lock(One Hand Operation+)과의 브로드캐스트/바로가기 전용 프로토콜 권장 UI 제공**
   * Good Lock 제스처의 "앱 실행" 대신 "빠른 도구 모음" 또는 "브로드캐스트 인텐트(`com.devdooly.notificationedge.OPEN_PANEL`)"를 등록하도록 설정 가이드 강화.

3. **대안 C: Quick Settings(상단 빠른 설정 타일) 기반 열기**
   * 상단 알림창의 빠른 설정 타일(`TileService`)을 제공하여 `startActivity` 없이 상단바 터치로 즉시 패널을 여는 기능 추가.

4. **대안 D: `FLAG_ACTIVITY_NO_USER_ACTION` 적용 검증**
   * 인텐트 플래그 중 `Intent.FLAG_ACTIVITY_NO_USER_ACTION`을 부여하여 액티비티 실행 시 `onUserLeaveHint()` 호출을 억제할 수 있는지 OS 버전별(Android 12/13/14) 테스트.

---

## 📌 이슈 #2: Google Play 최초 출시 준비

2026-09-05 기준 계획을 작성했으며, 앱 코드 변경이나 Play 제출은 아직 수행하지 않았다. 상세 작업 순서와 근거는 [Google Play 최초 출시 계획](GOOGLE_PLAY_LAUNCH_PLAN.md)을 따른다.

- [ ] 개발자 계정 유형·생성일·국가·검증 및 비공개 테스트 적용 여부 확인
- [ ] 기존 서명 계보의 최초 Play 등록 가능성과 실제 Play 업데이트 호환성 검증
- [ ] Play용 AAB/업데이트 경로 분리, 자체 APK 설치 권한·코드 제외
- [ ] API 36 및 최종 AAB·16KB·One UI 동작 검증
- [ ] 알림 접근 고지·동의, 개인정보처리방침·Data safety, FGS 신고·중지 UX 정리
- [ ] 실제 스토어 자료·라이선스·심사 시연·테스터 기록 준비

첫 공개판은 무광고를 권고한다. 접근성 서비스 추가 등 이전 연구 대안은 Play 정책 적합성을 별도 확인하기 전 출시 범위에 포함하지 않는다. GitHub 공개 상태는 유지하며 광고와 비공개 전환은 첫 출시 이후 별도 판단한다.
