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
 * 효율 (EfficiencyTrait) — 공용 수식어 '효율' 기질.
 *
 * 식각(강화) 효율을 ENCHANT_EFFICIENCY 배 증가시킨다.
 * Hero.getEnchantmentLevel()에서 식각 수치에 enchantEfficiency()를 곱연산.
 *
 * TODO: ENCHANT_EFFICIENCY 수치 확정
 */
public class EfficiencyTrait extends CommonTrait {

    private static final float ENCHANT_EFFICIENCY = 1.5f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public EfficiencyTrait()         { super(1); }
    public EfficiencyTrait(int tier) { super(tier); }

    @Override
    public float enchantEfficiency() {
        return ENCHANT_EFFICIENCY;
    }
}
