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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Fracture;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 골절 (FractureTrait) — 공용 수식어 '골절' 기질.
 *
 * 공격 명중 시 PROC_CHANCE 확률로 대상에게 '골절'(Fracture) 디버프를 부여한다.
 * Fracture 보유 대상은 가하는 피해가 감소한다 (Char.attack() 적용).
 *
 * TODO: PROC_CHANCE 수치 확정
 */
public class FractureTrait extends CommonTrait {

    private static final float PROC_CHANCE = 0.25f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public FractureTrait()         { super(1); }
    public FractureTrait(int tier) { super(tier); }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (damage > 0 && Random.Float() < PROC_CHANCE) {
            Buff.affect(defender, Fracture.class, Fracture.DURATION);
        }
        return damage;
    }
}
