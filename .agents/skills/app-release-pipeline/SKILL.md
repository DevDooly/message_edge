---
name: app-release-pipeline
description: >-
  Notification Edge 버전 판올림(4개 파일 동기화), 빌드 무결성 검증,
  Git 태그 생성, GitHub Actions CI/CD 모니터링 및 인앱 업데이트 배포 관리 전문 스킬.
  앱 배포, 버전 릴리즈, In-App Update 관리 시 활용.
---

# Android App Release Pipeline Skill

Notification Edge 프로젝트의 무결점 버전 판올림 및 GitHub Releases 배포 자동화 가이드입니다.

---

## 📋 1. 버전 판올림 동기화 체크리스트

버전을 올릴 때는 아래 **4개 파일/위치**를 빠짐없이 동시에 수정해야 합니다:

1. **`app/build.gradle.kts`**:
   - `versionCode = X` (예: 41)
   - `versionName = "1.X.X"` (예: "1.4.0")
2. **`app/src/main/java/com/devdooly/notificationedge/ui/settings/SettingsScreen.kt`**:
   - TopAppBar 버전 뱃지: `text = "v1.X.X"`
   - 인앱 업데이트 카드: `AppUpdateCard(currentVersionName = "1.X.X")`
   - 앱 정보 카드: `text = "버전 1.X.X (Build X) | Target Android 14"`

---

## 🚀 2. 표준 빌드 & 릴리즈 워크플로우

```bash
# 1. 로컬 단위 테스트 및 릴리즈 빌드 검증
./gradlew testDebugUnitTest assembleRelease

# 2. Git 변경 사항 스테이징 및 커밋 (한글 Conventional Commits)
git add .
git commit -m "feat: [기능 설명] (v1.X.X)"

# 3. Git 태그 생성
git tag -a v1.X.X -m "Release v1.X.X: [릴리즈 주요 변경 요약]"

# 4. 원격 저장소 푸시 (브랜치 및 태그)
git push origin main
git push origin v1.X.X
```

---

## 🌐 3. 배포 후 인앱 업데이트 확인

* GitHub Releases 배포 완료 후:
  - [https://github.com/DevDooly/message_edge/releases/latest](https://github.com/DevDooly/message_edge/releases/latest)
* 앱 내 설정 화면 ➔ [앱 업데이트] 카드 ➔ `[최신 업데이트 확인]`을 눌러 정상적으로 새 버전이 감지되는지 확인.
