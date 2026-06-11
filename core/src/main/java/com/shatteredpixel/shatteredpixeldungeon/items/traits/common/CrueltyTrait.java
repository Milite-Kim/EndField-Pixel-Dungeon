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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 잔혹 (CrueltyTrait) — 공용 수식어 '잔혹' 기질.
 *
 * 공격 명중 시 가한 피해의 BONUS_RATIO 만큼 추가 피해를 더한다.
 *
 * TODO: BONUS_RATIO 수치 확정
 */
public class CrueltyTrait extends CommonTrait {

    private static final float BONUS_RATIO = 0.15f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public CrueltyTrait()         { super(1); }
    public CrueltyTrait(int tier) { super(tier); }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (damage > 0) {
            damage += Math.max(1, Math.round(damage * BONUS_RATIO));
        }
        return damage;
    }
}
