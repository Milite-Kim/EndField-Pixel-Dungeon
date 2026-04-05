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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArmorBreaked;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 방어불능 스택 (DefenselessStack)
 *
 * 물리 이상 4종(띄우기/넘어뜨리기/강타/갑옷파괴)이 공유하는 스택 카운터.
 * 스택이 없을 때 물리 이상 적용 → 스택 +1만
 * 스택이 있을 때 물리 이상 적용 → 타입별 추가 효과 발동
 *
 * 사용법:
 *   DefenselessStack.apply(enemy, PhysicalAbnormality.LAUNCH);
 */
public class DefenselessStack extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 물리 이상 4종
    // ─────────────────────────────────────────────

    public enum PhysicalAbnormality {
        LAUNCH,        // 띄우기   - 스택 있을 때: +1스택 + 물리 피해
        KNOCKDOWN,     // 넘어뜨리기 - 스택 있을 때: +1스택 + 물리 피해 (+ 넉백 1칸, 추후 확정)
        HEAVY_ATTACK,  // 강타     - 스택 있을 때: 전량 소모 + 스택 비례 강타 피해
        ARMOR_BREAK    // 갑옷파괴  - 스택 있을 때: 전량 소모 + 스택 비례 약한 피해 + 물리 취약
    }

    public static final int MAX_STACKS = 4;

    private int stacks = 0;

    // ─────────────────────────────────────────────
    // 핵심 적용 메서드 (외부에서 이것만 호출)
    // ─────────────────────────────────────────────

    /**
     * 물리 이상을 적에게 적용한다.
     *
     * @param enemy 대상 적
     * @param type  물리 이상 종류 (LAUNCH / KNOCKDOWN / HEAVY_ATTACK / ARMOR_BREAK)
     */
    public static void apply(Char enemy, PhysicalAbnormality type) {
        DefenselessStack buff = enemy.buff(DefenselessStack.class);

        if (buff == null) {
            // 스택 없음 → 새로 생성, 1스택 부여
            buff = Buff.affect(enemy, DefenselessStack.class);
            buff.stacks = 1;

        } else {
            // 스택 있음 → 타입별 추가 효과
            switch (type) {

                case LAUNCH:
                case KNOCKDOWN:
                    // +1스택 (최대 4) + 해당 물리 피해
                    if (buff.stacks < MAX_STACKS) buff.stacks++;
                    buff.triggerLaunchOrKnockdown(enemy, type);
                    break;

                case HEAVY_ATTACK:
                    // 전량 소모 + 스택 비례 강타 피해
                    int heavyConsumed = buff.stacks;
                    buff.detach();
                    triggerHeavyAttack(enemy, heavyConsumed);
                    break;

                case ARMOR_BREAK:
                    // 전량 소모 + 스택 비례 약한 피해 + 물리 취약 디버프
                    int armorConsumed = buff.stacks;
                    buff.detach();
                    triggerArmorBreak(enemy, armorConsumed);
                    break;
            }
        }
    }

    // ─────────────────────────────────────────────
    // 타입별 효과 (수치는 추후 확정)
    // ─────────────────────────────────────────────

    private void triggerLaunchOrKnockdown(Char enemy, PhysicalAbnormality type) {
        // TODO: 스택 수에 비례한 물리 피해 구현
        // TODO: KNOCKDOWN의 경우 넉백 1칸 추가 (수치 미확정)
    }

    private static void triggerHeavyAttack(Char enemy, int consumedStacks) {
        // TODO: consumedStacks 비례 강력한 물리 피해 구현
    }

    private static void triggerArmorBreak(Char enemy, int consumedStacks) {
        // TODO: consumedStacks 비례 약한 물리 피해 구현
        // 갑옷 파괴 디버프 적용 (Vulnerable과 별개, 소모 스택 수 전달)
        ArmorBreaked.apply(enemy, consumedStacks);
    }

    // ─────────────────────────────────────────────
    // 스택 수 조회
    // ─────────────────────────────────────────────

    public int stacks() {
        return stacks;
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (턴마다 자동 소모 없음 - 소모형)
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
    public String iconTextDisplay() {
        return Integer.toString(stacks); // UI에 스택 수 표시
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
