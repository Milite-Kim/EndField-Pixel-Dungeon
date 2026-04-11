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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 운동 에너지 (KineticTrait) — 2티어 공용 기질.
 *
 * 공격 명중 시 PROC_CHANCE 확률로 대상에게 KNOCKDOWN(넘어뜨리기) +1스택.
 * 방어불능 스택 시스템과 연계하여 연계기 트리거 조건을 쌓는 데 유용하다.
 *
 * TODO: PROC_CHANCE 수치 확정
 */
public class KineticTrait extends CommonTrait {

    // ── 수치 (TODO: 확정) ─────────────────────────
    private static final float PROC_CHANCE = 0.15f; // 발동 확률 15%

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public KineticTrait() {
        super(2); // 2티어
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero && damage > 0 && Random.Float() < PROC_CHANCE) {
            DefenselessStack.apply(defender, DefenselessStack.PhysicalAbnormality.KNOCKDOWN, attacker);
        }
        return damage;
    }
}
