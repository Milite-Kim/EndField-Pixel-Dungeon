/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 동결 (Frozen) - 냉기 아츠 반응
 *
 * 타속성 부착 적에게 CRYO 부착 시 발동.
 * 소모 스택 비례 이동 불가 상태 적용.
 *
 *   1스택 소모   → 1턴 이동 불가
 *   2~3스택 소모 → 2턴 이동 불가
 *   4스택 소모   → 3턴 이동 불가
 *   고등급 적    → 면역
 *
 * 기존 SPD의 Frost와 별개의 독립 클래스.
 *
 * [특수 기믹 - 쇄빙]
 * 동결 상태 적에게 물리 이상 적용 시 → 동결 소모 + 대량 물리 피해
 * (DefenselessStack.apply()에서 Frozen 버프 체크 후 처리)
 */
public class Frozen extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    private int duration = 0;  // 남은 이동 불가 턴

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    public static void apply(Char enemy, int consumedStacks) {
        // TODO: 고등급 적 면역 체크 (등급 시스템 완성 후 구현)

        int dur = durationFromStacks(consumedStacks);
        Frozen buff = Buff.affect(enemy, Frozen.class);
        // 더 긴 쪽으로 갱신
        if (dur > buff.duration) {
            buff.duration = dur;
        }
    }

    /** 소모 스택 수에 따른 이동 불가 턴 수 */
    public static int durationFromStacks(int stacks) {
        if (stacks >= 4)        return 3;
        else if (stacks >= 2)   return 2;
        else                    return 1;
    }

    // ─────────────────────────────────────────────
    // 매 턴 동작: 이동 불가 (Paralysis 방식 참고)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        if (target.isAlive()) {
            if (duration > 0) {
                // 이동 및 행동 불가 처리
                target.paralysed++;   // Paralysis와 동일한 방식으로 행동 봉쇄
                duration--;
                spend(TICK);
            } else {
                target.paralysed = Math.max(0, target.paralysed - 1);
                detach();
            }
        } else {
            detach();
        }
        return true;
    }

    /** 동결 중인지 여부 (쇄빙 조건 체크용) */
    public boolean isFrozen() {
        return duration > 0;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 동결 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(duration);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String DURATION = "duration";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(DURATION, duration);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        duration = bundle.getInt(DURATION);
    }
}
