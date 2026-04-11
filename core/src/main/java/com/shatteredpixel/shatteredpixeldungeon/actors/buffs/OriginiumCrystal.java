/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.watabou.utils.Bundle;

/**
 * 오리지늄 아츠 결정 (Originium Arts Crystal)
 *
 * 관리자(Endministrator)의 연계기로 적에게 부여되는 버프.
 *
 * 소모 조건: 적이 물리 이상(DefenselessStack)을 받을 때 스택 1 소모 → 물리 피해
 * 마지막 스택 소모 시 버프 제거.
 *
 * 관리자 궁극기에서도 전량 소모 가능.
 *
 * TODO: 피해 수치 확정
 */
public class OriginiumCrystal extends Buff {

    /** 결정 소모 1회당 피해 배율. TODO: 수치 확정 */
    private static final float CONSUME_DMG_MULT = 0.5f;

    /**
     * 최대 스택 수.
     * 연계기 쿨타임보다 유지시간이 짧아 정상 플레이에서는 1스택 초과가 불가능하지만,
     * 층 이동 시 타이머가 멈추는 SPD 특성을 고려해 하드캡으로 명시.
     */
    public static final int MAX_STACKS = 1;

    /** 현재 스택 수 */
    private int stacks = 0;

    // ─────────────────────────────────────────────
    // 적용 (정적 진입점)
    // ─────────────────────────────────────────────

    /**
     * 적에게 오리지늄 아츠 결정을 부여한다.
     * 이미 버프가 있으면 스택을 추가한다.
     */
    public static void apply(Char enemy, int addStacks) {
        OriginiumCrystal crystal = Buff.affect(enemy, OriginiumCrystal.class);
        crystal.stacks = Math.min(crystal.stacks + addStacks, MAX_STACKS);
    }

    // ─────────────────────────────────────────────
    // 소모 (물리 이상 적중 시 자동 호출)
    // ─────────────────────────────────────────────

    /**
     * 물리 이상(DefenselessStack) 적중 시 결정 1스택 소모 → 물리 피해.
     * 스택이 0이 되면 버프 제거.
     *
     * @param enemy    결정 보유 적
     * @param attacker 물리 이상을 가한 공격자 (피해 계산 기준)
     */
    public void triggerConsumption(Char enemy, Char attacker) {
        if (stacks <= 0) {
            detach();
            return;
        }
        stacks--;

        int damage = Math.round(attacker.damageRoll() * CONSUME_DMG_MULT);
        enemy.damage(damage, attacker, DamageType.PHYSICAL);

        if (stacks <= 0) detach();
    }

    /**
     * 전량 소모 (궁극기 발동 시 호출).
     *
     * @return 소모된 스택 수 (추가 피해 계산에 사용)
     */
    public int consumeAll() {
        int consumed = stacks;
        stacks = 0;
        detach();
        return consumed;
    }

    /** 현재 스택 수 */
    public int stacks() { return stacks; }

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
