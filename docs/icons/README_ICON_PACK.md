# Notification Edge — Edge Whisper 앱 아이콘

## 컨셉

`화면 가장자리에서 알림 패널을 꺼내 바로 확인하고 답장한다`는 앱의 핵심 동작을
다음 네 요소로 압축했습니다.

- 말풍선 실루엣: 메시지·빠른 답장
- 3개의 알림 행과 상태 점: 최근 알림 목록
- 오른쪽 라이트 레일: 엣지 라이팅
- 중앙 제스처 핸들 및 왼쪽 화살표: 화면 가장자리에서 안쪽으로 당기는 동작

## 컬러

- PANTONE 11-4201 Cloud Dancer의 일반적인 디지털 근사값: `#F0EEE9`
- Graphite: `#151923`
- Aqueous Aqua: `#82D8D0`
- Quiet Periwinkle: `#A9A6EA`

Pantone 색상은 매체·소재·프로파일에 따라 달라질 수 있습니다.
인쇄나 엄격한 브랜드 컬러 매칭에는 Pantone Connect 또는 실물 스와치를 기준으로 확인하세요.

## 파일 구성

- `master/notification_edge_edge_whisper_master.svg`
  - 레이어가 분리된 편집용 원본
- `master/notification_edge_edge_whisper_1024.png`
  - 고해상도 마스터 PNG
- `play_store/notification_edge_edge_whisper_512.png`
  - Google Play 등록용 512×512 풀 스퀘어 PNG
- `preview/notification_edge_edge_whisper_preview.png`
  - 원형·스퀴클·라운드 마스크와 작은 크기 시인성 미리보기
- `android/app/src/main/res/...`
  - Adaptive Icon, Android 13+ Themed Icon, Legacy Icon 리소스 세트

## 현재 저장소에 적용

이 패키지는 현재 프로젝트의 Manifest에서 사용하는 리소스 이름
`@mipmap/ic_launcher`, `@mipmap/ic_launcher_round`에 맞춰져 있습니다.

1. `android/app/src/main/res/` 아래 폴더들을 프로젝트의
   `app/src/main/res/`에 복사합니다.
2. 기존 동명 파일은 교체합니다.
3. Android Studio에서 Clean/Rebuild 후 런처 미리보기를 확인합니다.

Android 8.0 이상에서는 Adaptive Icon이 사용되고,
Android 13 이상에서는 `monochrome` 레이어를 이용한 테마 아이콘도 지원합니다.

## 디자인 메모

- Play Store용 파일에는 바깥쪽 둥근 마스크나 외부 그림자를 굽지 않았습니다.
- Adaptive foreground는 108dp 캔버스의 중앙 안전영역에 맞게 축소했습니다.
- 심볼에는 글자를 넣지 않아 언어와 해상도에 관계없이 유지됩니다.
