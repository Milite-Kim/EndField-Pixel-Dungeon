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

/**
 * 어둠 (Darkness) — 공용 기질 '어둠'(ShadowTrait)이 부여하는 디버프.
 *
 * 보유 중인 대상의 명중률(공격 적중 판정)이 ACC_MULT 배로 감소한다.
 * 적용 지점: Char.hit() — 공격자 acuRoll 감소 (Hex와 동일 모델).
 *
 * TODO: ACC_MULT / DURATION 수치 확정
 */
public class Darkness extends FlavourBuff {

    /** 명중 판정 감소 배율 (acuRoll × 이 값). TODO: 수치 확정 */
    public static final float ACC_MULT = 0.7f;

    public static final float DURATION = 10f;

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    @Override
    public int icon() {
        return BuffIndicator.BLINDNESS;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
