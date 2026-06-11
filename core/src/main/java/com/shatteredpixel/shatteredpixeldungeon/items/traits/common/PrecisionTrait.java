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
 * 분쇄 (PrecisionTrait) — 공용 수식어 '분쇄' 기질.
 *
 * 명중률을 ACCURACY_MULT 배 증가시킨다.
 * Hero.attackSkill()에서 accuracyMultiplier()를 곱연산.
 *
 * TODO: ACCURACY_MULT 수치 확정
 */
public class PrecisionTrait extends CommonTrait {

    private static final float ACCURACY_MULT = 1.2f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public PrecisionTrait()         { super(1); }
    public PrecisionTrait(int tier) { super(tier); }

    @Override
    public float accuracyMultiplier() {
        return ACCURACY_MULT;
    }
}
