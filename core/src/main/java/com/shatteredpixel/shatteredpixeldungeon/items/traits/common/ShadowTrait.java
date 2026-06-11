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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Darkness;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 어둠 (ShadowTrait) — 공용 수식어 '어둠' 기질.
 *
 * 공격 명중 시 PROC_CHANCE 확률로 대상에게 '어둠'(Darkness) 디버프를 부여한다.
 * Darkness 보유 대상은 명중률이 감소한다 (Char.hit() 적용).
 *
 * TODO: PROC_CHANCE 수치 확정
 */
public class ShadowTrait extends CommonTrait {

    private static final float PROC_CHANCE = 0.25f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public ShadowTrait()         { super(1); }
    public ShadowTrait(int tier) { super(tier); }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (damage > 0 && Random.Float() < PROC_CHANCE) {
            Buff.affect(defender, Darkness.class, Darkness.DURATION);
        }
        return damage;
    }
}
