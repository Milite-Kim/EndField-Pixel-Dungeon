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
 * 골절 (Fracture) — 공용 기질 '골절'(FractureTrait)이 부여하는 디버프.
 *
 * 보유 중인 대상이 가하는 피해가 DAMAGE_MULT 배로 감소한다.
 * 적용 지점: Char.attack() — 공격력 감소 (Weakness와 동일 모델).
 *
 * TODO: DAMAGE_MULT / DURATION 수치 확정
 */
public class Fracture extends FlavourBuff {

    /** 공격력 감소 배율 (가하는 피해 × 이 값). TODO: 수치 확정 */
    public static final float DAMAGE_MULT = 0.8f;

    public static final float DURATION = 10f;

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    @Override
    public int icon() {
        return BuffIndicator.WEAKNESS;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
