# Notification Edge — 슬라이딩 패널 앱 아이콘

## 디자인 컨셉

화면 가장자리에서 알림 패널을 넣고 빼는 앱의 동작을 작은 아이콘 안에 압축했습니다.

- 세 개의 알림 행: 최근 메시지와 빠른 확인
- 오른쪽 컬러 레일: 화면 엣지와 앱의 핵심 진입점
- 왼쪽 양각 화살표: 엣지에서 패널을 꺼내는 동작
- 오른쪽 음각 화살표: 패널을 다시 엣지로 접는 동작
- 짙은 미드나이트 배경: 밝은 홈 화면에서도 안정적인 대비

중앙 돌출부는 단순 손잡이 대신 양각·음각 화살표 한 쌍으로 구성했습니다. 한쪽은 컬러 레일 밖으로 나오고 다른 쪽은 레일 내부를 파내어, `열기 ↔ 닫기` 동작을 동시에 전달합니다.

## 컬러

| 역할 | 색상 |
| --- | --- |
| 배경 | `#060B1E` → `#0A1733` |
| 알림 패널 | `#102744` |
| 알림 행 | `#17385F` |
| 알림 내용 | `#F0EEE9` |
| 엣지 레일 | `#00D7D7` → `#4387FF` → `#7C5CFC` |

알림 내용에는 PANTONE 11-4201 Cloud Dancer의 디지털 근사값 `#F0EEE9`를 사용했습니다. 인쇄나 엄격한 브랜드 컬러 매칭에는 Pantone Connect 또는 실물 스와치를 기준으로 확인해야 합니다.

## 적용 파일

- `master/notification_edge_sliding_panel_master.svg`: 편집용 컬러 벡터
- `master/notification_edge_sliding_panel_monochrome.svg`: 테마 아이콘용 단색 벡터
- `master/notification_edge_sliding_panel_1024.png`: 1024×1024 마스터 PNG
- `play_store/notification_edge_sliding_panel_512.png`: Google Play 등록용 512×512 PNG
- `preview/notification_edge_sliding_panel_preview.png`: 스퀴클 적용 미리보기
- `preview/notification_edge_sliding_panel_monochrome.png`: Android 13 이상 테마 아이콘 미리보기
- `source/notification_edge_sliding_panel_concept.png`: 사용자가 선택한 원본 시안
- `app/src/main/res/drawable*/ic_launcher_*`: Adaptive Icon 배경·전경·단색 레이어
- `app/src/main/res/mipmap*/ic_launcher*`: Android 7.1 이하 호환 런처 아이콘

## 재생성

아이콘 크기나 색을 변경한 뒤 저장소 루트에서 다음 명령을 실행하면 밀도별 PNG와 문서용 이미지를 다시 만들 수 있습니다.

```powershell
.\scripts\generate-launcher-assets.ps1
```

벡터 XML과 SVG의 도형을 변경한 경우 재생성 스크립트의 동일 좌표도 함께 맞춰야 합니다.

## 안드로이드 지원 범위

- Android 8.0 이상: 기기 런처의 원형·스퀴클·물방울 마스크에 대응하는 Adaptive Icon
- Android 13 이상: Material You 색상을 적용할 수 있는 Monochrome Icon
- Android 7.1 이하: 밀도별 일반·원형 PNG 아이콘
- 핵심 심볼은 108dp 아트보드의 66dp 안전영역 안에 배치해 런처 마스크 잘림을 방지했습니다.
