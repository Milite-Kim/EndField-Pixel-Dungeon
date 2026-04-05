/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 연소 (Combustion) - 열기 아츠 반응
 *
 * 타속성 부착 적에게 HEAT 부착 시 발동.
 * 소모된 스택 수에 비례한 열기 피해를 매 턴 가한다.
 *
 * 기존 SPD의 Burning과 별개의 독립 클래스.
 */
public class Combustion extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    private int consumedStacks = 1;  // 소모된 아츠 부착 스택 수
    private int duration       = 0;  // 남은 지속 턴

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    public static void apply(Char enemy, int consumedStacks) {
        Combustion buff = Buff.affect(enemy, Combustion.class);
        // 더 강한 연소로 갱신
        if (consumedStacks >= buff.consumedStacks) {
            buff.consumedStacks = consumedStacks;
            buff.duration = baseDuration(consumedStacks);
        }
    }

    /** 소모 스택 비례 지속 시간 (TODO: 수치 확정) */
    private static int baseDuration(int stacks) {
        return 2 + stacks; // 예: 1스택=3턴, 4스택=6턴
    }

    /** 소모 스택 비례 매 턴 열기 피해 (TODO: 수치 확정) */
    private int damagePerTick() {
        return consumedStacks; // TODO: 실제 수치로 교체
    }

    // ─────────────────────────────────────────────
    // 매 턴 동작: 열기 피해 적용
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        if (target.isAlive()) {
            if (duration > 0) {
                // TODO: 열기 속성 피해 처리 (속성 피해 시스템 완성 후 교체)
                target.damage(damagePerTick(), this);
                duration--;
                spend(TICK);
            } else {
                detach();
            }
        } else {
            detach();
        }
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 연소 아이콘
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
