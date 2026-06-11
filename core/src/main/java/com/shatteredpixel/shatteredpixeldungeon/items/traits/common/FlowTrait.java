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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 흐름 (FlowTrait) — 공용 수식어 '흐름' 기질.
 *
 * 공격 명중 시 메인 오퍼레이터의 궁극기를 CHARGE_PER_HIT 만큼 추가 충전한다.
 *
 * TODO: CHARGE_PER_HIT 수치 확정
 */
public class FlowTrait extends CommonTrait {

    private static final int CHARGE_PER_HIT = 1; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public FlowTrait()         { super(1); }
    public FlowTrait(int tier) { super(tier); }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero) {
            Hero hero = (Hero) attacker;
            if (hero.activeUltimate != null) {
                hero.activeUltimate.addCharge(CHARGE_PER_HIT);
            }
        }
        return damage;
    }
}
