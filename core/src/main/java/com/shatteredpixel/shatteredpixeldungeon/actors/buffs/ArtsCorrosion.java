/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 부식 (ArtsCorrosion) - 자연 아츠 반응
 *
 * 타속성 부착 적에게 NATURE 부착 시 발동.
 * 이 디버프가 붙은 적을 공격하면 메인 오퍼레이터가 소량 회복.
 *
 * 기존 SPD의 Corrosion(산성)과 별개의 독립 클래스.
 *
 * 사용법:
 *   회복량 계산: ArtsCorrosion.healOnHit(consumedStacks)
 *   공격 적중 시 Hero.java 또는 공격 처리 코드에서 체크:
 *     if (enemy.buff(ArtsCorrosion.class) != null) { hero.heal(...) }
 */
public class ArtsCorrosion extends Buff {

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
        ArtsCorrosion buff = Buff.affect(enemy, ArtsCorrosion.class);
        if (consumedStacks >= buff.consumedStacks) {
            buff.consumedStacks = consumedStacks;
            buff.duration = baseDuration(consumedStacks);
        }
    }

    /** 소모 스택 비례 지속 시간 (TODO: 수치 확정) */
    private static int baseDuration(int stacks) {
        return 3 + stacks; // TODO: 수치 확정
    }

    /** 공격 적중 시 회복량 (TODO: 수치 확정) */
    public int healOnHit() {
        return consumedStacks; // TODO: 실제 수치로 교체
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
        return BuffIndicator.NONE; // TODO: 부식 아이콘
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
