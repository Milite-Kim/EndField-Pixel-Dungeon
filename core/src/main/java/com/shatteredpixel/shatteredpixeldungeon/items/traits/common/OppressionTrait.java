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
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 억제 (OppressionTrait) — 공용 수식어 '억제' 기질.
 *
 * 대상이 디버프(NEGATIVE 버프)를 하나라도 보유 중이면 가하는 피해가 BONUS_MULT 배 증가한다.
 * 엔픽던 특성상 방어불능/아츠 부착 등으로 대부분의 적이 디버프 상태가 되므로 범용 딜증.
 *
 * TODO: BONUS_MULT 수치 확정
 */
public class OppressionTrait extends CommonTrait {

    private static final float BONUS_MULT = 1.2f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public OppressionTrait()         { super(1); }
    public OppressionTrait(int tier) { super(tier); }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (damage > 0 && hasDebuff(defender)) {
            damage = Math.round(damage * BONUS_MULT);
        }
        return damage;
    }

    private static boolean hasDebuff(Char ch) {
        for (Buff b : ch.buffs()) {
            if (b.type == Buff.buffType.NEGATIVE) return true;
        }
        return false;
    }
}
