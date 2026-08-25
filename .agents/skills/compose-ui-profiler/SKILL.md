---
name: compose-ui-profiler
description: >-
  Jetpack Compose UI/UX 렌더링 성능 최적화, 불필요한 Recomposition 방지,
  커스텀 폰트 비동기 로드, 글래스모피즘(Glassmorphism) 및 120fps 애니메이션 튜닝 전문 스킬.
  Compose UI 컴포넌트 개발, 애니메이션, 테마 및 성능 튜닝 시 활용.
---

# Jetpack Compose UI/UX Profiler & Optimization Skill

Notification Edge 프로젝트의 부드러운 120fps 엣지 패널 렌더링, 알림 카드 애니메이션, 커스텀 폰트 렌더링의 성능 최적화 원칙을 제공합니다.

---

## 🎨 1. Compose 성능 최적화 핵심 원칙

### 1) 불필요한 Recomposition 방지
* **안정적인 타입(Stable Types) 사용**:
  - `List<EdgeNotification>` 대신 불변 컬렉션 또는 `Immutable` 어노테이션 적용 고려.
* **람다 인스턴스화 최소화**:
  - 이벤트 콜백(`onClose`, `onItemClick`)은 매 recomposition마다 새로 생성되지 않도록 `remember`로 래핑.
* **상태 읽기 지연(Defer State Reads)**:
  - 오프셋이나 알파 애니메이션 적용 시 `Modifier.graphicsLayer { alpha = animatedAlpha }`와 같이 람다 기반 modifier를 사용하여 렌더링 단계에서만 상태를 소비.

### 2) 커스텀 폰트 SAF 로드 최적화
* 사용자 업로드 커스텀 폰트(`.ttf`, `.otf`)는 `files/fonts/`에 저장 후,
* `FontFamily(Font(File(...)))` 생성 결과를 메모리에 캐싱하여 recomposition 시 중복 파일 I/O 발생 차단.

### 3) 엣지 패널 글래스모피즘(Glassmorphism) 렌더링
* 안드로이드 12+ (API 31) 이상에서는 `Modifier.blur()` 활용.
* 이하 버전에서는 반투명 `Surface(color = DarkSurface.copy(alpha = 0.85f))` 및 그라데이션 테두리(`BorderStroke`)로 하드웨어 가속 유지.

---

## 🚀 2. 120fps 애니메이션 베스트 프랙티스

```kotlin
// 엣지 패널 등장 슬라이드 애니메이션
val panelOffset by animateDpAsState(
    targetValue = if (isVisible) 0.dp else (-panelWidthDp).dp,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    label = "panelSlide"
)
```

---

## ⚠️ 3. UI 체크리스트
* [ ] 알림 목록 스크롤 시 Jank(프레임 드랍)가 발생하지 않는가?
* [ ] 다크 테마 및 고대비 텍스트 가독성이 유지되는가?
* [ ] 시스템 폰트 크기 변경(크게/작게) 시 UI 레이아웃이 깨지지 않는가?
