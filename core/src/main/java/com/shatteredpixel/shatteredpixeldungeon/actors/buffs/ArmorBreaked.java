/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 갑옷 파괴 (ArmorBreaked)
 *
 * DefenselessStack의 ARMOR_BREAK 소모 시 발동되는 디버프.
 * 물리 취약(Vulnerable)과는 별개의 독립 개념.
 * 주로 포그라니치니크의 배틀스킬을 통해 발동됨.
 *
 * - 지속 시간: 고정 (TODO: 수치 확정)
 * - 효과: 물리 피해 증가 (소모 스택 비례, Char.java에서 체크)
 *
 * 사용법:
 *   ArmorBreaked.apply(enemy, consumedStacks);
 */
public class ArmorBreaked extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // TODO: 지속 시간 수치 확정
    public static final float DURATION = 5f;

    // 소모된 방어불능 스택 수 (피해 배율 계산에 사용)
    private int consumedStacks = 1;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    /**
     * 갑옷 파괴 디버프를 적에게 적용한다.
     * 이미 존재하면 스택 수와 지속 시간을 갱신한다.
     *
     * @param consumedStacks 소모된 방어불능 스택 수 (1~4)
     */
    public static void apply(com.shatteredpixel.shatteredpixeldungeon.actors.Char enemy, int consumedStacks) {
        ArmorBreaked buff = Buff.affect(enemy, ArmorBreaked.class, DURATION);
        // 이미 존재하면 더 높은 스택 수로 갱신
        if (consumedStacks > buff.consumedStacks) {
            buff.consumedStacks = consumedStacks;
        }
    }

    // ─────────────────────────────────────────────
    // 피해 배율 계산 (Char.java에서 호출)
    // ─────────────────────────────────────────────

    /**
     * 소모 스택 수에 따른 물리 피해 증가 배율.
     * TODO: 수치 확정 후 조정
     *
     * 예시 (미확정):
     *   1스택 소모 → 1.10f (10% 증가)
     *   2스택 소모 → 1.20f
     *   3스택 소모 → 1.30f
     *   4스택 소모 → 1.40f
     */
    public float damageMult() {
        return 1f + 0.1f * consumedStacks; // TODO: 수치 확정
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (FlavourBuff와 동일하게 시간 차감)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 전용 아이콘 추가 시 교체
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString((int) visualcooldown());
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String CONSUMED_STACKS = "consumedStacks";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CONSUMED_STACKS, consumedStacks);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        consumedStacks = bundle.getInt(CONSUMED_STACKS);
    }
}
