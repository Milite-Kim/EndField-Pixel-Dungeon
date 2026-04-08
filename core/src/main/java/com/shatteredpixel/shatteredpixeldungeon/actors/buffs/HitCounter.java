/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 피격 횟수 카운터 (HitCounter)
 *
 * Hero가 적을 타격할 때마다 해당 적에게 누적되는 카운터 버프.
 *
 * [아케쿠리 연계기 조건 연동]
 *   누적 피격 횟수가 THRESHOLD에 도달하면:
 *   1. 카운터 리셋 (재사용 가능)
 *   2. thresholdJustHit = true (context flag 패턴)
 *   3. Dungeon.hero.checkChainTriggers(this) 호출
 *   4. thresholdJustHit = false
 *
 *   아케쿠리의 chainCondition은 thresholdJustHit를 확인해 트리거.
 *
 * [아비웨나 연계기 조건 연동 — 최초 공격]
 *   hitCount가 처음으로 1이 됐을 때:
 *   1. firstHitContext = true
 *   2. Dungeon.hero.checkChainTriggers(this) 호출
 *   3. firstHitContext = false
 *
 *   아비웨나의 chainCondition은 isFirstHitJust()를 확인해 트리거.
 *
 * [연동 클래스]
 *   - Char.java → damage() 내부에서 HitCounter.increment(this) 호출
 *   - Akekuri.java → chainCondition에서 isThresholdJustHit() 확인
 *   - Aviena.java  → chainCondition에서 isFirstHitJust() 확인
 *
 * 이 버프는 자동 생성·영구 지속 (적이 살아있는 동안).
 * 적 사망 시 detach는 Buff 기본 동작에 의해 자동 처리.
 */
public class HitCounter extends Buff {

    { type = buffType.NEUTRAL; announced = false; }

    // ─────────────────────────────────────────────
    // 상수
    // ─────────────────────────────────────────────

    /** 연계기 발동까지 필요한 누적 피격 횟수. TODO: 수치 확정 */
    public static final int THRESHOLD = 10;

    // ─────────────────────────────────────────────
    // 상태 필드
    // ─────────────────────────────────────────────

    private int hitCount = 0;

    /**
     * 임계치 도달 직후 순간적으로 true가 되는 컨텍스트 플래그.
     * checkChainTriggers() 호출 도중에만 true이며, 반환 후 즉시 false 복구.
     * 직렬화 불필요 (로드 시 항상 false).
     */
    private transient boolean thresholdJustHit = false;

    /**
     * 최초 타격(hitCount가 0→1) 직후 순간적으로 true가 되는 컨텍스트 플래그.
     * checkChainTriggers() 호출 도중에만 true. 아비웨나 연계기 조건 체크용.
     */
    private transient boolean firstHitContext = false;

    // ─────────────────────────────────────────────
    // 외부 호출 진입점
    // ─────────────────────────────────────────────

    /**
     * Hero가 enemy에게 피해를 줄 때 Char.damage()에서 호출.
     * 카운터를 생성(없으면)하고 1 증가.
     */
    public static void increment(Char enemy) {
        if (enemy == null || !enemy.isAlive()) return;
        HitCounter counter = Buff.affect(enemy, HitCounter.class);
        counter.addHit();
    }

    // ─────────────────────────────────────────────
    // 내부 로직
    // ─────────────────────────────────────────────

    private void addHit() {
        hitCount++;

        // 최초 타격 이벤트 (0→1): 아비웨나 연계기 트리거
        if (hitCount == 1) {
            firstHitContext = true;
            if (Dungeon.hero != null) {
                Dungeon.hero.checkChainTriggers(target);
            }
            firstHitContext = false;
        }

        if (hitCount >= THRESHOLD) {
            hitCount = 0;
            // context flag 패턴: checkChainTriggers 도중에만 true
            thresholdJustHit = true;
            if (Dungeon.hero != null) {
                Dungeon.hero.checkChainTriggers(target);
            }
            thresholdJustHit = false;
        }
    }

    /** 임계치 도달 여부. chainCondition에서 호출. */
    public boolean isThresholdJustHit() {
        return thresholdJustHit;
    }

    /** 최초 타격 여부. 아비웨나 chainCondition에서 호출. */
    public boolean isFirstHitJust() {
        return firstHitContext;
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (영구 — 소모 없음)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE;
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String HIT_COUNT = "hitCount";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(HIT_COUNT, hitCount);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        hitCount = bundle.getInt(HIT_COUNT);
        // thresholdJustHit은 transient — 로드 시 항상 false
    }
}
