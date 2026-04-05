/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 감전 (Electrified) - 전기 아츠 반응
 *
 * 타속성 부착 적에게 ELECTRIC 부착 시 발동.
 * 지속 시간 동안 받는 아츠 피해 증가.
 *
 * 피해 배율은 Char.java 피해 계산에서 체크.
 * (ArmorBreaked와 동일한 방식으로 적용)
 */
public class Electrified extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    private int consumedStacks = 1;
    private int duration       = 0;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    public static void apply(Char enemy, int consumedStacks) {
        Electrified buff = Buff.affect(enemy, Electrified.class);
        if (consumedStacks >= buff.consumedStacks) {
            buff.consumedStacks = consumedStacks;
            buff.duration = baseDuration(consumedStacks);
        }
    }

    /** 소모 스택 비례 지속 시간 (TODO: 수치 확정) */
    private static int baseDuration(int stacks) {
        return 2 + stacks; // TODO: 수치 확정
    }

    /**
     * 소모 스택 비례 아츠 피해 증가 배율.
     * Char.java 피해 계산 시 호출됨.
     * TODO: 수치 확정
     */
    public float artsDamageMult() {
        return 1f + 0.1f * consumedStacks; // TODO: 수치 확정
    }

    // ─────────────────────────────────────────────
    // 매 턴 동작: 지속 시간 차감
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        if (target.isAlive() && duration > 0) {
            duration--;
            spend(TICK);
        } else {
            detach();
        }
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 감전 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(duration);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String CONSUMED_STACKS = "consumedStacks";
    private static final String DURATION        = "duration";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CONSUMED_STACKS, consumedStacks);
        bundle.put(DURATION,        duration);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        consumedStacks = bundle.getInt(CONSUMED_STACKS);
        duration       = bundle.getInt(DURATION);
    }
}
