# 🤖 Notification Edge 개발 지침 및 프로젝트 규칙 (GEMINI.md)

이 문서는 Antigravity 및 모든 AI 에이전트가 본 프로젝트(Notification Edge)에서 작업할 때 **항상 자동으로 로드되어 준수해야 하는 최상위 개발 지침**입니다.

---

## 📌 1. 필수 사용자 규칙 (User Rules)

* **언어 표준**: Git 커밋 메시지, 태그 메시지, README 등 모든 마크다운(`*.md`) 문서는 **반드시 한글로 작성**해야 합니다.
* **자동 Git 워크플로우**: 코드 수정 및 기능 구현 후 빌드 검증(`compileDebugKotlin assembleRelease`)을 수행하고, 별도의 추가 확인 요청 없이 **로컬 커밋 및 원격 저장소(`git push origin main` 및 태그 푸시)까지 자동으로 진행**해야 합니다.

---

## 🧰 2. 프로젝트 전용 Antigravity 스킬 (.agents/skills/)

본 프로젝트에는 전문적인 개발과 회귀 방지를 위해 다음 4가지 전용 스킬이 구축되어 있습니다:

| 스킬명 | 경로 | 설명 및 활용 시점 |
| :--- | :--- | :--- |
| **`android-overlay-expert`** | [`.agents/skills/android-overlay-expert/SKILL.md`](.agents/skills/android-overlay-expert/SKILL.md) | 오버레이 윈도우, 투명 호스트 액티비티, `NotificationListenerService`, 안드로이드 8~15 포커스/뒤로가기 제어 시 활용 |
| **`compose-ui-profiler`** | [`.agents/skills/compose-ui-profiler/SKILL.md`](.agents/skills/compose-ui-profiler/SKILL.md) | Jetpack Compose UI 성능 최적화, 불필요한 Recomposition 제거, 120fps 애니메이션 튜닝 시 활용 |
| **`android-test-automation`** | [`.agents/skills/android-test-automation/SKILL.md`](.agents/skills/android-test-automation/SKILL.md) | 단위/회귀 테스트 자동화, Compose UI/DataStore 테스트 작성, 회귀 방지(Regression Defense) 시 활용 |
| **`app-release-pipeline`** | [`.agents/skills/app-release-pipeline/SKILL.md`](.agents/skills/app-release-pipeline/SKILL.md) | 버전 판올림 4곳 동기화, GitHub Release 배포 및 인앱 업데이트 무결성 관리 시 활용 |

---

## 📚 3. 핵심 참조 문서 (Reference Documents)

1. **[`docs/DEVELOPMENT_REFERENCE.md`](docs/DEVELOPMENT_REFERENCE.md)**: 전체 아키텍처 다이어그램, 이전 버전 트러블슈팅 내역, 빌드 최적화 및 배포 가이드
2. **[`docs/PENDING_ISSUES.md`](docs/PENDING_ISSUES.md)**: 추후 해결할 기술적 과제 및 미해결 이슈 목록 (예: 유튜브 전체화면 PiP 완화 연구 등)

---

## 🏗 4. 핵심 아키텍처 및 주의사항

1. **엣지 패널 투명 액티비티 아키텍처 (`EdgePanelActivity`)**:
   - 엣지 패널은 완전 투명 무애니메이션 액티비티(`Theme.NotificationEdge.TranslucentPanel`)인 `EdgePanelActivity`로 구동되어, **안드로이드 OS 레벨의 네비게이션 뒤로가기(하단 소프트키 버튼 및 화면 가장자리 스와이프 제스처)를 100% 보장**합니다.
   - `MainActivity.kt`는 `Theme.NoDisplay` 무화면 트램펄린으로 0ms에 패널을 실행하고 즉시 종료됩니다.
   - 무거운 Compose 설정 UI는 `SettingsActivity.kt`에서 구동됩니다.

2. **알림 파싱 및 중복 접두어 제거**:
   - 메시지 알림은 반드시 `NotificationTextCleaner.cleanMessageText`를 통과시켜 발신자 이름이나 전화번호가 본문에 중복으로 나타나지 않도록 유지합니다.
   - 1:1 대화에서는 본문 발신자 라벨을 생략하고, 단체 카톡방에서만 화자 구분 라벨을 표시합니다.

---

## 🔄 5. 표준 버전 판올림 절차

새로운 기능 추가나 버그 수정 후 릴리즈 시:
1. `app/build.gradle.kts` (`versionCode`, `versionName`) 수정
2. `app/src/main/java/com/devdooly/notificationedge/ui/settings/SettingsScreen.kt` (TopAppBar 뱃지, AppUpdateCard, AppInfoCard) 수정
3. 빌드 검증: `./gradlew testDebugUnitTest assembleRelease`
4. 커밋 & 푸시:
   ```bash
   git add .
   git commit -m "타입: 한글 설명(v버전)"
   git tag -a v버전 -m "Release v버전: 상세 설명"
   git push origin main
   git push origin v버전
   ```
