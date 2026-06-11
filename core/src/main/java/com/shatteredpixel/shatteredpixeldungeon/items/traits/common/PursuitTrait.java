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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PursuitBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 추격 (PursuitTrait) — 공용 수식어 '추격' 기질.
 *
 * 공격 명중 시 공격자에게 PursuitBuff(이동 속도 증가)를 일정 턴 부여한다.
 *
 * TODO: PursuitBuff.DURATION / SPEED_MULT 수치 확정
 */
public class PursuitTrait extends CommonTrait {

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public PursuitTrait()         { super(1); }
    public PursuitTrait(int tier) { super(tier); }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero) {
            Buff.affect(attacker, PursuitBuff.class, PursuitBuff.DURATION);
        }
        return damage;
    }
}
