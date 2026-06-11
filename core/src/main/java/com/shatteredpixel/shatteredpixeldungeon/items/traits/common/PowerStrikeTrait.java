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
 * 강공 (PowerStrikeTrait) — 공용 수식어 '강공' 기질.
 *
 * 기본 공격 피해를 ATTACK_MULT 배 증가시킨다.
 * Hero.finalAttackEfficiency()에서 basicAttackMultiplier()를 곱연산.
 *
 * TODO: ATTACK_MULT 수치 확정 (티어별 차등 여부 포함)
 */
public class PowerStrikeTrait extends CommonTrait {

    private static final float ATTACK_MULT = 1.15f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public PowerStrikeTrait()         { super(1); }
    public PowerStrikeTrait(int tier) { super(tier); }

    @Override
    public float basicAttackMultiplier() {
        return ATTACK_MULT;
    }
}
