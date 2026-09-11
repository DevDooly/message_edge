# PiP 자동 전환 완화 작업 기록

## 상태 — 2026-09-11, 실제 One Hand Operation+ 동작 확인·v1.3.24 공개 배포 완료

사용자는 Good Lock / One Hand Operation+ 제스처로 Slivue를 실행한다. 영상 재생 중 자동 PiP 전환을 원하지 않으며, 영상을 일시정지하는 방식도 허용했다.

**사용자가 ‘목록 확인되고, 실제로 pip 변경 안되고 잘 동작함’이라고 확인했다.** 연결된 Galaxy S23 Ultra / Android 16 / One UI 8.5의 `홈 화면 바로가기 → Slivue: 알림 패널 열기` 경로는 검증 완료이며, 이를 [v1.3.24 / 빌드 154](https://github.com/DevDooly/message_edge/releases/tag/v1.3.24)로 공개 배포했다. 기존 `앱 실행` 경로와 모든 영상 앱·OS 조합의 해결을 의미하지는 않는다.

사용자가 같은 기기에서 **Slivue 핸들로 열면 PiP가 발생하지 않지만, One Hand Operation+의 앱 실행으로 열면 작은 창으로 바뀐다**고 확인했다. 일시정지 시점 변경만으로 원래 제스처 경로를 해결하지 못했다.

추가로 사용자가 보낸 One Hand Operation+ 화면에서 `홈 화면 바로가기` 목록에는 `YouTube: 검색`, `Chrome: 새 탭` 등의 앱 바로가기만 있고 Slivue는 없었다. 두 번째 시험판의 `ACTION_CREATE_SHORTCUT` 결과 반환은 정상 동작했지만, 이 목록이 소비하는 시스템 앱 바로가기 등록을 빠뜨렸다. 이전 안내대로 선택할 수 없었던 것은 앱의 연동 구현 문제이며, 사용자 설정 실수가 아니다.

## 발견한 원인과 수정안

- 기존 코드는 `EdgePanelActivity`가 열린 다음 설정을 읽어 유튜브에 일시정지를 요청했다. 초기값도 꺼짐이었다.
- 공통 실행 경로 `EdgePanelLauncher`에서 영상 일시정지를 먼저 요청하고 최대 600ms 동안 정지 상태를 확인한 뒤 패널을 연다. 확인된 경우 80ms의 짧은 반영 여유를 둔다. 메인 스레드를 대기시키지 않는다.
- 활성 미디어 세션의 우선순위대로 PiP를 선언한 영상 앱 한 개만 처리한다. APK의 매니페스트를 공개 리소스 API로 읽으며, 비공개 ActivityInfo 비트나 전체 패키지 조회 권한은 사용하지 않는다. 런처 앱을 조회 범위로 선언했다.
- 이미 멈춘 세션·음악 전용 제외 목록·Slivue 자체·조회 불가 앱은 건드리지 않는다. 재생/정지 토글, 전역 미디어키, 자동 재생은 사용하지 않는다.
- 준비 중 중복 열기, 닫기 후 뒤늦은 열기를 막는다. 명시적으로 꺼 둔 기존 설정은 보존하고, 미설정 기본값만 켜짐으로 바꾼다.
- 문서와 달리 실제 런처 테마가 일반 투명 테마를 상속하고 있어 `Theme.NoDisplay` 상속으로 맞췄다. 진입 액티비티는 대기하지 않고 즉시 종료한다. 무거운 UI는 계속 별도 액티비티에 둔다.
- Slivue에서 패널을 여는 인텐트에는 `FLAG_ACTIVITY_NO_USER_ACTION`을 추가했다. 이는 외부 앱이 이미 보낸 최초 실행 인텐트까지 바꾸지는 못한다.
- `OpenPanelActivity`에 누락됐던 `ACTION_CREATE_SHORTCUT` 결과 반환을 구현했다. 등록 시 패널이나 설정을 열지 않고, 이름·아이콘·실행 인텐트를 호출 앱에 반환한다. 결과 수신이 취소되지 않도록 이 등록 진입점의 실행 모드는 `standard`로 변경했다. 메인 런처와 패널의 `singleInstance` 구조는 유지한다.
- 바로가기 실행 인텐트에도 `FLAG_ACTIVITY_NO_USER_ACTION`을 포함한다. 이 플래그가 최초 진입에서 보존되는 경우를 겨냥한 우회 경로이며, One Hand Operation+가 플래그를 그대로 사용하는지는 실제 제스처 시험이 필요하다. 공개 API인 [바로가기 결과 생성](https://developer.android.com/reference/androidx/core/content/pm/ShortcutManagerCompat#createShortcutResultIntent(android.content.Context,androidx.core.content.pm.ShortcutInfoCompat))을 사용한다.
- 세 번째 시험판에서는 `PanelShortcuts`가 `slivue_open_panel` 동적 앱 바로가기를 `ShortcutManager`에 등록한다. 표시 이름은 한국어 `알림 패널 열기`, 영어 `Open notification panel`이며 런처의 `MainActivity`에 연결한다. 실제 실행 대상은 기존 `OpenPanelActivity`다. 최초 실행 플래그가 유지되어야 하므로 사용자 지정 플래그를 지원하지 않는 정적 XML 바로가기는 사용하지 않았다. 근거: [앱 바로가기와 실행 플래그](https://developer.android.com/develop/ui/compose/system/shortcuts/managing-shortcuts).
- 앱 실행·업데이트·언어 변경 시 이 ID만 등록 또는 갱신한다. 이미 최신이면 다시 쓰지 않고, 다른 바로가기는 삭제하지 않는다. 잠금 상태나 시스템 등록 실패가 패널을 막지 않으며 이후 앱 실행에서 재시도한다. 기존 바로가기 생성 API도 호환용으로 유지한다.

## 확인한 결과와 한계

| 검사 | 결과 |
| --- | --- |
| 단위·회귀 검사 | 최종 전체 재실행 207개 통과, 실패·오류 0개 |
| 전체 빌드 | `testDebugUnitTest compileDebugKotlin lintRelease assembleRelease assembleMinifiedRelease assembleDebugAndroidTest` 통과 |
| Lint | 오류 0개, 경고 67개, 정보 1개 |
| 일반·축소·계측 APK 서명 | 기존 서명 계보 확인 |
| Android 16 기존 계측 검사 | 5개 통과 |
| 합성 영상 앱 → Slivue 핸들 탭 | 정지 상태 및 PiP 진입 0회 확인 |
| 합성 영상 앱 → 외부 액티비티 실행 방식 | 일시정지는 됐지만, 먼저 PiP에 들어가는 사례 재현 |
| 삼성 실기기 핸들 실행 | 사용자 확인: PiP 전환 없음 |
| 삼성 실기기 One Hand Operation+ 앱 실행 | 사용자 확인: 여전히 PiP로 전환됨 |
| 최초 인텐트에 `NO_USER_ACTION`을 포함한 합성 시험 | 영상 정지, 패널 정상 실행, 닫은 뒤 PiP 진입 0회 |
| 합성 앱의 바로가기 등록 → 반환된 인텐트 실행 | 등록 결과 수신 성공, 수동 진입·자동 진입 설정 모두 영상 정지 및 PiP 0회, 뒤로가기로 정상 복귀 |
| 두 번째 시험판의 One Hand Operation+ 목록 표시 | 사용자 화면 확인: Slivue 없음. 동적 앱 바로가기 등록 누락 |
| 세 번째 시험판의 삼성 시스템 앱 바로가기 등록 | 동적 바로가기 1개, 활성 상태 확인. 기기 출력은 이름·ID를 가림 |
| 세 번째 시험판의 `LauncherApps` 조회·실행 | 가상 기기에서 등록 항목 조회 성공, 수동/자동 PiP 설정 모두 정지·PiP 0회 |
| 세 번째 시험판의 One Hand Operation+ 목록 및 실제 실행 | 사용자 확인: 목록 표시됨, 실제 영상 재생 중 PiP 전환 없이 정상 동작 |

계측 APK에만 `PipPlaybackFixtureActivity`를 추가했다. 실제 계정·영상·음성 없이 MediaSession과 PiP 콜백을 재현한다. 공개 앱 APK에는 이 테스트 화면이 포함되지 않는다. 시험 전 이미 PiP에 떠 있는 상태와 새로 전체 화면에서 시작하는 상태를 구분했다.

세 번째 시험은 계측 앱에 **가상 기기에서만** 임시 HOME 역할을 부여해 `LauncherApps.getShortcuts/startShortcut` 실제 API를 호출했다. 같은 조건에서 일반 앱 실행은 PiP 진입이 재현됐지만 등록된 동적 바로가기 실행은 정지 후 PiP 0회였다. 시험 후 원래 Pixel Launcher로 HOME 역할을 복원했다. 삼성폰의 기본 홈 앱이나 PiP 권한은 변경하지 않았다.

전체 검사의 첫 실행에서는 기존 `SettingsViewModelTest` 1개가 실패했다. Windows 임시 DataStore 파일 이동의 `AccessDeniedException`과 60초 대기 초과가 함께 기록됐다. 해당 테스트 단독 실행 및 전체 207개 재실행은 코드 변경 없이 통과했다. 이 환경 의존 실패를 새 바로가기 기능의 성공으로 덮거나 테스트를 생략하지 않았다.

합성 앱의 `onUserLeaveHint()` 즉시 PiP 진입 경로에서는 외부에서 Slivue 액티비티를 시작하는 순간 PiP 요청이 먼저 발생한다. Slivue 내부에서 정지를 앞당겨도 이 첫 요청을 취소하는 일반 앱용 API는 없다. 이 결과를 모든 실제 앱의 동작과 같다고 단정하지는 않지만, **Good Lock 완전 해결을 주장할 수 없는 반례**다.

`setAutoEnterEnabled`를 사용하는 자동 진입과 앱이 직접 `enterPictureInPictureMode`를 호출하는 경로도 구분해야 한다. 근거: [Android PiP 가이드](https://developer.android.com/develop/ui/views/picture-in-picture), [인텐트 실행 플래그](https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NO_USER_ACTION), [미디어 세션 접근 권한](https://developer.android.com/reference/android/media/session/MediaSessionManager).

## 시험판 설치 이력과 최종 확인

연결된 Galaxy S23 Ultra에 기존 서명을 유지한 전용 바로가기 포함 두 번째 시험판을 업데이트 설치했다. 기기 기록상 설치 시각은 2026-09-11 09:34:49이며, APK SHA-256은 `4b70499f45c1815c6c9cb30dc41c20d8c64f49b016bb4a6f3efdbfd80a478fb7`이다. 버전 표시는 아직 v1.3.23 / 빌드 153이며 공개 v1.3.23 APK와는 다른 시험 빌드다. 첫 시험판 설정 화면에서 `패널 열기 전 영상 일시 정지`가 켜져 있음을 확인했고, 두 번째 시험판도 앱 삭제·데이터 초기화 없이 설치했다. 실제 One Hand Operation+ 설정은 자동 변경하지 않았다.

이후 **세 번째 시험판**을 2026-09-11 09:57:34에 같은 삼성폰에 업데이트 설치했다. APK SHA-256은 `a054f839af826b4595f4d36cc7ddc8a4da9ae951f608df3e80dac1da8c2e4de6`이며, 버전 표시는 시험용 v1.3.23 / 빌드 153이었다. `cmd shortcut get-shortcuts --user 0 --flags 15 com.devdooly.notificationedge`로 활성 동적 항목을 확인했다. 이 명령의 필터 값과 `LauncherApps.ShortcutQuery` 상수 값은 혼동하지 않는다. 삼성 출력의 가려진 ID·이름을 우회해 읽지 않았다. 이후 사용자가 실제 목록 표시와 PiP 전환 없이 정상 동작함을 확인했다.

1. 완료: Android 16 합성 영상 앱이 `startActivityForResult`로 바로가기를 등록하고, 반환받은 인텐트를 그대로 실행하는 종단 간 시험을 통과했다. `auto_pip=false`의 `onUserLeaveHint` 방식과 `auto_pip=true`의 자동 진입 설정 모두 PiP 진입 0회를 확인했다. 실제 유튜브 구현이나 삼성 제스처 앱의 플래그 보존 여부를 대신 보장하지 않는다.
2. 완료: 사용자가 `Slivue: 알림 패널 열기` 목록 표시와 실제 제스처 동작을 확인했다. 기존 선택은 자동 변환되지 않는다.
3. 사용자는 실제 영상 재생 중 PiP 전환 없음을 확인했다. 모든 앱의 일시정지 상태·가로/분할 화면·장시간 동작을 검증한 것으로 확대하지 않는다. 접근성 권한 추가, 전역 미디어 제어, PiP 권한 임의 변경, 영상 화면 강제 재실행은 하지 않았다.
4. v1.3.24 버전·문서·앱 안내를 갱신하고 최종 빌드·서명 및 CI의 API 26/35 업데이트 검증을 거쳐 공개 배포한다.

로컬 시험 캡처는 버전 관리에서 제외되는 `app/build/manual-tests/pip/`에 보관한다.

## v1.3.24 최종 로컬 검증

- 버전 1.3.24 / 빌드 154에서 필수 전체 빌드 및 일반·축소·계측 APK 서명 검증을 다시 통과했다. 단위·회귀 207개, Android 16 일반 릴리스 대상 계측 5개 모두 성공했다.
- Lint 오류 0개, 경고 67개, 정보 1개다. 이 최종 빌드에서는 앞서 기록한 임시파일 접근 실패가 재현되지 않았다.
- Android 16 가상 기기에 업데이트 설치한 뒤 버전 154와 동적 바로가기의 ID·이름·`0x10050000` 실행 플래그 유지도 확인했다.
- 로컬 일반 APK SHA-256: `b8bdb1fdf9046d192c45878dacfe157a718035be5c0a5c5b670be2aa642dc73c`. CI에서 만드는 공개 APK의 해시는 별도로 검증한다.
- 삼성폰은 사용자 확인 이후 USB 연결이 해제되어 최종 154 APK를 직접 설치하지 않았다. 사용자 실기기 PiP 확인은 기능이 같은 세 번째 시험판에 대한 결과다.
- 태그 CI의 API 26·35 제자리 업데이트 검증도 통과했다.

## 공개 APK와 CI 결과

- 제품 커밋: `e710dba`, 태그 `v1.3.24`. [태그 CI 34549636264](https://github.com/DevDooly/message_edge/actions/runs/34549636264)의 빌드·서명·API 26/35 기존 설치 업데이트·공개 배포가 모두 성공했다.
- [CodeQL 검사 34549633999](https://github.com/DevDooly/message_edge/actions/runs/34549633999) 통과.
- 실제 공개 APK와 첨부 체크섬을 다시 내려받아 CI 검증 후보와 동일함을 확인했다. SHA-256: `87b68382ffc1ffc5a711258647511a3ae7a2dad9d9cffcf4b3a04a51000c7438`.
- APK 서명 인증서와 기존 계보를 확인했다. 공개 파일과 동일한 후보로 Android 8 계측 5개 및 앱 바로가기 등록을 추가 검증했다. Android 16에도 공개 APK를 업데이트 설치해 기존 계측 5개를 재확인했다.
- 공개 릴리스는 정식·최신 릴리스로 제공하며, `Slivue.apk`와 버전명 APK에 동일한 체크섬이 연결된다. 릴리스 안내는 한글 문서로 반영했다.

### 별도 main CI의 간헐적 테스트 실패 보완

동일 제품 코드의 태그 CI는 성공했으나 [최초 main CI 34549633954](https://github.com/DevDooly/message_edge/actions/runs/34549633954)는 `SettingsViewModelTest`의 60초 대기 초과로 실패했다. 앞서 Windows에서 확인한 임시 DataStore 경로 문제와 같은 테스트에서 재현됐으며, Linux 로그에서는 동일 대기 초과까지만 확인했다.

ViewModel의 ‘저장소에 위임한다’ 단위 검사를 파일 I/O에서 분리해 MockK로 정확한 호출·인자를 검증하도록 보완했다. 실제 영속화 검사는 `SettingsRepositoryTest`에 유지하고, 일시정지·햅틱 두 설정의 독립적인 꺼짐/켜짐 저장을 추가 확인한다. 테스트 삭제나 제한 시간 증가는 하지 않았다. 수정 후 로컬 필수 전체 빌드와 207개 검사를 통과했으며, 이 후속 변경은 테스트와 문서에만 해당하므로 배포된 앱 동작 코드는 바뀌지 않는다.
