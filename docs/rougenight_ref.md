# 명픽던(RougeNight / TomorrowRogueNight) 코드 분석

> 참조 경로: `reference/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/`
> 분석 목적: SPD 기반 Arknights 모드의 설계 결정을 파악하고 엔픽던 설계에 참고

---

## 1. 영웅/오퍼레이터 시스템

### SPD 원본과의 차이: `HeroClass.java`

SPD 원본 4클래스(WARRIOR, MAGE, ROGUE, HUNTRESS)에 Arknights 캐릭터 3개 추가:

| 클래스 | 서브클래스 | 시작 무기 | 시작 아티팩트 | 특이사항 |
|--------|-----------|----------|-------------|---------|
| ROSECAT | DESTROYER / GUARDIAN / WAR | EX42 | AnnihilationGear | 고기 2개로 시작 |
| NEARL | KNIGHT / SAVIOR / FLASH | NEARL_AXE | **SealOfLight** | 방어막 → 공격 연계 |
| CHEN | SWORDMASTER / SPSHOOTER | ChenSword | — | 강화 물약으로 시작 |

**클래스별 초기화 구조** (`initRosecat()`, `initNearl()`, `initChen()`):
```java
private void initNearl(Hero hero) {
    (hero.belongings.weapon = new NEARL_AXE()).identify();
    (hero.belongings.artifact = new SealOfLight()).identify();
    // 나이트 스킬, 재능 세팅 ...
}
```

### 스킨 시스템: `Hero.java`

```java
public int CharSkin = 0; // 17개 스킨 지원
// 0: 기본, 1: TALULAH, 2: F_NOVA, 3: SKADI, ...
```
- 스킨별 스프라이트 클래스 분기 (`sprites/skins/` 폴더)
- 게임 내에서 스킨 변경 가능

### 3단계 스킬 슬롯: `Hero.java`

```java
public Skill SK1, SK2, SK3; // 3개 스킬 슬롯 (레벨별 분리)
```
- `items/Skill/SK1/`, `SK2/`, `SK3/` 각 20개 이상의 스킬
- `SkillBook` 아이템으로 스킬 관리
- 현재 엔픽던 방식(BattleSkill/Ultimate)과 근본적으로 다른 접근

---

## 2. 스킬/콤보 시스템 (가장 중요)

### 핵심 패턴: `Buff implements ActionIndicator.Action`

명픽던의 모든 활성 스킬은 **Buff + ActionIndicator.Action 인터페이스** 구조로 구현됨:

```java
// Buff를 액션 버튼으로도 쓰는 패턴
public class Combo extends Buff implements ActionIndicator.Action {
    public Image getIcon();   // UI에 표시할 아이콘
    public void doAction();   // 버튼 클릭 시 실행
    public void tintIcon(Image icon); // 현재 상태에 따라 아이콘 색 변경
    public float iconFadePercent();   // 타이머 시각화 (0=꽉참, 1=비어)
}
```

`hit(Char enemy)` 호출 → 스택/타이머 갱신 → `ActionIndicator.setAction(this)` → UI에 버튼 표시

### ActionIndicator ↔ 엔픽던 ChainQueue 대응 관계

명픽던의 ActionIndicator 패턴과 우리 ChainQueue 시스템은 같은 문제를 다른 방식으로 푼다.

| 항목 | 명픽던 ActionIndicator | 엔픽던 ChainQueue |
|------|----------------------|-----------------|
| 버튼 표시 트리거 | 버프가 `setAction(this)` 직접 호출 (이벤트 푸시) | HUD가 `peek()` 매 프레임 폴링 |
| 버튼 제거 트리거 | 버프가 `clearAction(this)` 직접 호출 | peek() == null이면 HUD가 숨김 |
| 타이머 시각화 | `iconFadePercent()` (0=꽉참, 1=비어) | timerBar + timerLabel (수동) |
| 버튼 로직 위치 | 버프 클래스 내부 (`doAction()`) | HUD 내부 (`ChainBtn.onClick()`) |

**리팩터링 방향 (나중에)**: ChainQueue가 `enqueue()`할 때 `ActionIndicator.setAction(chainAction)`을,
`consume()`·만료 시에 `ActionIndicator.clearAction()`을 직접 호출하면 HUD 폴링 없이 동일한 UX 구현 가능.
현재 ChainBtn의 timerBar·timerLabel 로직은 `iconFadePercent()`로 대체 가능.

---

### 기본 콤보 시스템: `actors/buffs/Combo.java`

- **스택 기반 무브 해금**: 공격할 때마다 count 증가, 5초 타임아웃
- **5개 ComboMove**, 각각 요구 스택이 다름:

| 무브 | 최소 스택 | 효과 | tintColor |
|------|---------|------|-----------|
| CLOBBER | 2 | 넉백 + 어지러움 | 0xFF8080 |
| SLAM | 4 | count * 20% 피해 | 0xFF8000 |
| PARRY | 6 | 특수 방어 | 0x8080FF |
| CRUSH | 8 | 3칸 AOE | 0xFF4040 |
| FURY | 10 | 스택×다중타격 | 0xFF0000 |

```java
public void hit(Char enemy) {
    count++;
    comboTime = 5f; // 리셋
    if (getHighestMove() != null)
        ActionIndicator.setAction(this);
}
```

### Knight(Nearl) 스킬: `actors/buffs/KnightSKILL.java`

별도 카운터 버프(`KnightSkillCombo extends CounterBuff`)와 짝:
- KnightSKILL: 타이머 + 무브 선택 담당
- KnightSkillCombo: 스택 카운팅 담당 (저장/불러오기 포함)

| 무브 | 스택 | 효과 | 특이사항 |
|------|------|------|---------|
| KNOCKBACK | — | 넉백 + 침묵 | 재능 3단계 필요 |
| SMASH | — | 강화 공격 | — |
| KILLBLOW | — | 0.7× 피해 | 처치 시 SealOfLight +3충전 |
| LIGHTSWORD | 스택 | 스택당 0.25×(또는 0.35×) | — |

### Chen 스킬: `actors/buffs/ChenCombo.java`

- 3개 무브: SKILL1(마비+취약) / SKILL2(직선빔3칸) / SKILL3(최대4회 타격)
- `hits` 변수로 남은 타격 횟수 추적

### CounterBuff 패턴: `actors/buffs/CounterBuff.java`

```java
// 스택 카운팅 전용 Buff (저장/불러오기 포함)
public class CounterBuff extends Buff {
    private float count = 0;
    public void countUp(float inc) { count += inc; }
    public void countDown(float inc) { count -= inc; }
    public float count() { return count; }
    // storeInBundle / restoreFromBundle 자동 처리
}
```
스택 추적이 필요한 모든 버프의 베이스 클래스로 활용.

---

## 3. ActionIndicator UI 시스템

### `ui/ActionIndicator.java`

`Tag`를 상속하여 우측 HUD에 버튼 형태로 표시:

```java
public class ActionIndicator extends Tag {
    public static Action action;  // 현재 활성 액션 (static)

    public interface Action {
        Image getIcon();
        void doAction();
        // 선택적:
        default void tintIcon(Image icon) {}
        default float iconFadePercent() { return 0; }
        default String actionName() { return ""; }
    }

    public static void setAction(Action action) { ... }
    public static void clearAction(Action action) { ... }
}
```

- `action`이 null이 아니면 버튼 visible
- 탭하면 `action.doAction()` 호출 → 보통 WndCombo 같은 팝업 표시
- `iconFadePercent()`로 타이머 시각화 (fade-out 효과)

---

## 4. 전투 계산 수정

### `actors/Char.java`

피해 처리 순서 변경/추가:
```java
public void damage(int dmg, Object src) {
    // 1. ChampionEnemy 배율 적용
    for (ChampionEnemy buff : buffs(ChampionEnemy.class))
        dmg = (int) Math.ceil(dmg * buff.damageTakenFactor());

    // 2. 방어막 흡수 (ShieldBuff 순서대로)
    for (ShieldBuff s : buffs(ShieldBuff.class)) {
        dmg = s.absorbDamage(dmg);
        if (dmg == 0) break;
    }
    HP -= dmg;
}
```

`speed()` 수정 예시:
```java
if (buff(LanceCharge.class) != null) speed *= 5f; // 5배 이동속도 버프
```

`attackProc()` 수정 예시:
```java
if (hero.hasTalent(Talent.RESTRICTION)) {
    float restr = 1f - (hero.pointsInTalent(Talent.RESTRICTION) * 0.05f);
    damage *= restr;
}
```

### `actors/buffs/ActiveOriginium.java`

오리지늄 중독 도트 데미지 공식:
```java
// 턴당: level * 0.2f + HP * 0.05f
int dmg = (int)(level() * 0.2f + target.HP * 0.05f);
```

---

## 5. UI 추가 요소

### 스킬별 전용 팝업 창

| 창 | 역할 |
|----|------|
| `windows/WndCombo.java` | 기본 Warrior 콤보 무브 선택 |
| `windows/WndKnightSkill.java` | Knight 스킬 무브 선택 |
| `windows/WndChenCombo.java` | Chen 스킬 무브 선택 |

각 팝업은 사용 가능한 무브 목록을 보여주고 선택하면 `ActionIndicator.doAction()` 호출.

### `ui/StatusPane.java`

```java
// talentBlink: 재능 습득 시 깜빡임 표시
if (talentBlink) { ... }
// CharSkin 기반 아바타 선택
```

---

## 6. 아티팩트: SealOfLight (Nearl 전용)

`ShieldSlamCounter extends CounterBuff`와 연계:
- 방어막으로 피해 흡수할 때마다 카운터 증가
- KILLBLOW로 처치 시 `addCharge(3)` 호출
- 카운터가 임계값 도달 → 방어막 생성

---

## 7. 엔픽던에서 참고하면 좋은 부분 (우선순위 순)

### 🔴 높은 우선순위

| 우리 코드 | 참고할 명픽던 코드 | 이유 |
|-----------|-----------------|------|
| `operators/ChainQueue.java` + `ui/CombatHUD.java` (ChainBtn) | `ui/ActionIndicator.java` | ChainQueue의 enqueue/consume에서 setAction/clearAction 호출 시 폴링 제거 가능. 상세 분석은 "ActionIndicator ↔ ChainQueue 대응 관계" 섹션 참고 |
| `actors/buffs/DefenselessStack.java` (스택 추적) | `actors/buffs/CounterBuff.java` | 스택 저장/불러오기가 자동화된 CounterBuff 베이스를 활용하면 코드 단순화 가능 |
| `ui/CombatHUD.java` (SkillBtn 쿨다운) | `actors/buffs/KnightSKILL.java`의 `comboTime` + `iconFadePercent()` | 쿨다운 남은 시간을 시각화하는 패턴. 현재 SkillBtn은 오버레이 클리핑 + 남은 턴 숫자 표시로 구현됨 ✅ |

### 🟡 중간 우선순위

| 우리 코드 | 참고할 명픽던 코드 | 이유 |
|-----------|-----------------|------|
| `operators/BattleSkill.java` (멀티히트) | `actors/buffs/ChenCombo.java`의 `hits` 변수 + `Callback` 체인 | 진천우 궁극기 7연타처럼 재귀 콜백 체인으로 멀티히트 구현 패턴 |
| `scenes/OperatorSelectScene.java` | `windows/WndClass.java`, `scenes/TitleScene.java` | 클래스 선택 → 정보 표시 → 확정 UX 흐름 |
| `operators/TeamOperator.java`의 연계기 트리거 | `actors/buffs/Combo.java`의 `hit(Char)` 훅 | 공격 적중 시 스택/조건 체크하는 표준 패턴 |

### 🟢 낮은 우선순위 (나중에)

| 우리 코드 | 참고할 명픽던 코드 | 이유 |
|-----------|-----------------|------|
| 오퍼레이터 스프라이트 시스템 | `sprites/skins/` 폴더 전체 | 플레이어 스프라이트 교체 방법 |
| 기질 아이템 시스템 | `items/Skill/SK1/` 폴더 | SkillBook + Skill 베이스의 아이템형 스킬 관리 |
| 보스 전투 설계 | `actors/mobs/` Tomimi, Raptor 등 보스 파일 | 다단계 보스 패턴 |

---

## 8. 명픽던이 채택하지 않은 것 (우리가 독자적으로 만든 것)

| 시스템 | 엔픽던 설계 | 명픽던 설계 |
|--------|-----------|-----------|
| 속성 반응 (DamageType) | PHYSICAL/HEAT/COLD/NATURE/ELECTRIC 타입별 리액션 | 없음 (표준 SPD 피해) |
| 방어불능 스택 (DefenselessStack) | LAUNCH/KNOCKDOWN/HEAVY_ATTACK 이상 체계 | 없음 |
| 연계기 큐 (ChainQueue) | 팀원 순차 발동 시스템 | 없음 (개인 콤보만) |
| 오퍼레이터 선택 분리 (Main+Team) | 2단계 선택 | HeroClass 단일 선택 |
| 궁극기 충전 시스템 (Ultimate) | 배틀스킬/일반공격으로 충전 | 없음 |

---

## 참조 파일 경로 빠른 색인

```
reference/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/
├── actors/
│   ├── buffs/
│   │   ├── Combo.java          ← 콤보 시스템 핵심 + ActionIndicator.Action 구현 예시
│   │   ├── KnightSKILL.java    ← 멀티무브 스킬 버프 구현 예시
│   │   ├── KnightSkillCombo.java ← CounterBuff 활용 예시
│   │   ├── ChenCombo.java      ← 멀티히트 콜백 체인 예시
│   │   ├── CounterBuff.java    ← 스택 카운팅 베이스 클래스
│   │   ├── LanceCharge.java    ← 속도 배율 버프 구현 예시
│   │   └── ActiveOriginium.java ← 도트 데미지 공식 참고
│   ├── hero/
│   │   ├── Hero.java           ← SK1/SK2/SK3 슬롯, CharSkin 스킨 시스템
│   │   └── HeroClass.java      ← 오퍼레이터별 초기화 패턴
│   └── Char.java               ← 피해 처리 순서, 속도 버프 적용 방법
├── ui/
│   └── ActionIndicator.java    ← 스킬 버튼 UI 구현 참고
└── windows/
    ├── WndCombo.java           ← 스킬 선택 팝업 레이아웃
    ├── WndKnightSkill.java     ← 무브 선택 팝업
    └── WndClass.java           ← 클래스 정보 표시 창
```
