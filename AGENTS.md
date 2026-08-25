# 🤖 Notification Edge 개발 지침 및 프로젝트 규칙 (AGENTS.md)

이 문서는 Antigravity 및 모든 AI 에이전트가 본 프로젝트(Notification Edge)에서 작업할 때 **항상 자동으로 로드되어 준수해야 하는 최상위 개발 지침**입니다.

---

## 📌 1. 필수 사용자 규칙 (User Rules)

* **언어 표준**: Git 커밋 메시지, 태그 메시지, README 등 모든 마크다운(`*.md`) 문서는 **반드시 한글로 작성**해야 합니다.
* **자동 Git 워크플로우**: 코드 수정 및 기능 구현 후 빌드 검증(`compileDebugKotlin assembleRelease`)을 수행하고, 별도의 추가 확인 요청 없이 **로컬 커밋 및 원격 저장소(`git push origin main` 및 태그 푸시)까지 자동으로 진행**해야 합니다.

---

## 📚 2. 핵심 참조 문서 (Reference Documents)

작업 전 반드시 다음 문서들을 확인하고 가이드를 준수하십시오:

1. **[`docs/DEVELOPMENT_REFERENCE.md`](docs/DEVELOPMENT_REFERENCE.md)**:
   - 전체 아키텍처 다이어그램 및 주요 컴포넌트 역할
   - 이전 버전들의 트러블슈팅 내역 및 해결 방식
   - 표준 빌드, 버전 판올림, Git 배포 명령어 모음
2. **[`docs/PENDING_ISSUES.md`](docs/PENDING_ISSUES.md)**:
   - 추후 해결할 기술적 과제 및 미해결 이슈 목록 (예: 유튜브 전체화면 PiP 완화 연구 등)

---

## 🏗 3. 핵심 아키텍처 및 주의사항

1. **NoDisplay 트램펄린 아키텍처 (`MainActivity` vs `SettingsActivity`)**:
   - `MainActivity.kt`는 `Theme.NoDisplay` 테마로 실행되며, 화면을 띄우지 않고 0ms 동기로 `EdgeOverlayService`를 호출 후 즉시 종료됩니다.
   - 무거운 Compose 설정 UI는 반드시 `SettingsActivity.kt`에서만 구동되어야 합니다.

2. **오버레이 윈도우 및 뒤로가기(Back) 키 처리**:
   - `EdgeOverlayService.kt`의 윈도우 파라미터는 `FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS`를 사용합니다.
   - `FLAG_NOT_FOCUSABLE`을 부여하면 안드로이드 시스템의 뒤로가기(`KEYCODE_BACK`) 키/제스처가 오버레이 창으로 들어오지 않으므로 주의하십시오.
   - 뒤로가기 처리는 `OverlayPanelLayout.kt`의 `dispatchKeyEventPreIme` 및 `dispatchKeyEvent`를 통해 보장됩니다.

3. **알림 파싱 및 중복 접두어 제거**:
   - 메시지 알림을 처리할 때는 반드시 `NotificationTextCleaner.cleanMessageText`를 통과시켜 발신자 이름이나 전화번호가 본문에 중복으로 나타나지 않도록 유지해야 합니다.
   - 1:1 대화에서는 본문 발신자 라벨을 생략하고, 단체 카톡방에서만 화자 구분 라벨을 표시합니다.

---

## 🔄 4. 표준 버전 판올림 절차

새로운 기능 추가나 버그 수정 후 릴리즈 시:
1. `app/build.gradle.kts` (`versionCode`, `versionName`) 수정
2. `app/src/main/java/com/devdooly/notificationedge/ui/settings/SettingsScreen.kt` (TopAppBar 뱃지, AppUpdateCard, AppInfoCard) 수정
3. 빌드 검증: `./gradlew compileDebugKotlin assembleRelease`
4. 커밋 & 푸시:
   ```bash
   git add .
   git commit -m "타입: 한글 설명(v버전)"
   git tag -a v버전 -m "Release v버전: 상세 설명"
   git push origin main
   git push origin v버전
   ```
