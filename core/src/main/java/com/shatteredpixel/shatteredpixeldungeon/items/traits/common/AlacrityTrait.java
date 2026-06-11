/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.traits.common;

import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 사기 (AlacrityTrait) — 공용 수식어 '사기' 기질.
 *
 * 공격 속도를 ATTACK_SPEED_MULT 배 증가시킨다 (공격 딜레이 감소).
 * Hero.operatorAttackDelay()에서 delay를 attackSpeedMultiplier()로 나눔.
 *
 * TODO: ATTACK_SPEED_MULT 수치 확정
 */
public class AlacrityTrait extends CommonTrait {

    private static final float ATTACK_SPEED_MULT = 1.2f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public AlacrityTrait()         { super(1); }
    public AlacrityTrait(int tier) { super(tier); }

    @Override
    public float attackSpeedMultiplier() {
        return ATTACK_SPEED_MULT;
    }
}
