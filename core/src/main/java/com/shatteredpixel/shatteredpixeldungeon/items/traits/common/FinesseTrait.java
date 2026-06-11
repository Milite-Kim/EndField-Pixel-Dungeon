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
 * 기예 (FinesseTrait) — 공용 수식어 '기예' 기질.
 *
 * 스킬(배틀스킬·궁극기·연계기) 피해를 SKILL_MULT 배 증가시킨다.
 * Hero.skillDamageRoll()에서 skillDamageMultiplier()를 곱연산.
 *
 * TODO: SKILL_MULT 수치 확정
 */
public class FinesseTrait extends CommonTrait {

    private static final float SKILL_MULT = 1.15f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public FinesseTrait()         { super(1); }
    public FinesseTrait(int tier) { super(tier); }

    @Override
    public float skillDamageMultiplier() {
        return SKILL_MULT;
    }
}
