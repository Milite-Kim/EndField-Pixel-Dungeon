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
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 의료 (MendingTrait) — 공용 수식어 '의료' 기질.
 *
 * 공격 명중 시 PROC_CHANCE 확률로 가한 피해의 HEAL_RATIO 만큼 HP를 회복한다.
 *
 * TODO: PROC_CHANCE / HEAL_RATIO 수치 확정
 */
public class MendingTrait extends CommonTrait {

    private static final float PROC_CHANCE = 0.25f; // TODO: 수치 확정
    private static final float HEAL_RATIO  = 0.20f; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public MendingTrait()         { super(1); }
    public MendingTrait(int tier) { super(tier); }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (damage > 0 && Random.Float() < PROC_CHANCE) {
            int heal = Math.max(1, Math.round(damage * HEAL_RATIO));
            attacker.HP = Math.min(attacker.HP + heal, attacker.HT);
            attacker.sprite.showStatus(CharSprite.POSITIVE, "+" + heal);
        }
        return damage;
    }
}
