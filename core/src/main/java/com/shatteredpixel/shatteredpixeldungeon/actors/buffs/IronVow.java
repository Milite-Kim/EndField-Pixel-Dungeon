/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 철의 서약 (IronVow)
 *
 * 포그라니치니크(Pogranichnik)의 궁극기로 적에게 부여되는 버프.
 * 최초 3스택으로 부여.
 *
 * 소모 조건:
 *   - 적이 물리 이상(DefenselessStack)을 받을 때 1스택 소모
 *   - 연계기가 적에게 적중할 때 1스택 소모
 *
 * 소모 효과:
 *   - 일반: 물리 피해(×TRIGGER_DMG_MULT)
 *   - 마지막 스택(3→0): 물리 피해(×FINAL_DMG_MULT) — 대량 피해
 *
 * TODO: 피해 수치 확정
 */
public class IronVow extends Buff {

    /** 일반 소모 피해 배율. TODO: 수치 확정 */
    private static final float TRIGGER_DMG_MULT = 0.5f;

    /** 마지막 스택 소모 대량 피해 배율. TODO: 수치 확정 */
    private static final float FINAL_DMG_MULT   = 2.5f;

    private int stacks = 3;

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 적용 (정적 진입점)
    // ─────────────────────────────────────────────

    /** 적에게 철의 서약 n스택을 부여한다. 이미 있으면 스택을 덮어씀(갱신). */
    public static void apply(Char enemy, int stacks) {
        IronVow vow = Buff.affect(enemy, IronVow.class);
        vow.stacks = stacks;
    }

    // ─────────────────────────────────────────────
    // 소모 (물리 이상 / 연계기 적중 시 호출)
    // ─────────────────────────────────────────────

    /**
     * 철의 서약 1스택 소모 → 물리 피해.
     * 마지막 스택 소모 시 대량 피해로 대체.
     *
     * @param enemy    서약 보유 적
     * @param attacker 트리거를 유발한 공격자
     */
    public void trigger(Char enemy, Char attacker) {
        if (stacks <= 0) {
            detach();
            return;
        }

        boolean isLast = (stacks == 1);
        stacks--;

        float mult = isLast ? FINAL_DMG_MULT : TRIGGER_DMG_MULT;
        int damage = Math.round(attacker.damageRoll() * mult);
        enemy.damage(damage, attacker, DamageType.PHYSICAL);

        if (stacks <= 0) detach();
    }

    public int stacks() { return stacks; }

    @Override
    public int icon() {
        return BuffIndicator.DEGRADE; // TODO: 전용 아이콘 추가
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String STACKS = "stacks";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STACKS, stacks);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        stacks = bundle.getInt(STACKS);
    }
}
