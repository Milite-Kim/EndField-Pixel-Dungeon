/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.traits.special;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.SpecialTrait;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Endministrator;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 오리지늄 공진 (EndministratorResonanceTrait) — 관리자 전용 특수 기질 (5티어).
 *
 * [관리자 메인 시]
 *   공격이 방어불능 스택 보유 적을 명중할 때 추가 물리 피해.
 *   스택 수가 많을수록 추가 피해 증가:
 *     추가 피해 = damage × BONUS_MULT_PER_STACK × stacks
 *
 * [다른 오퍼레이터 메인 시]
 *   5티어 공용 기질처럼 기본 proc 없이 식각 수치만 기여.
 *
 * TODO: BONUS_MULT_PER_STACK 수치 확정
 */
public class EndministratorResonanceTrait extends SpecialTrait {

    // ── 수치 (TODO: 확정) ─────────────────────────
    /** 방어불능 스택 1개당 추가 물리 피해 배율 */
    private static final float BONUS_MULT_PER_STACK = 0.15f;

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    @Override
    public Class<? extends Operator> operatorClass() {
        return Endministrator.class;
    }

    @Override
    public int operatorProc(Char attacker, Char defender, int damage) {
        DefenselessStack ds = defender.buff(DefenselessStack.class);
        if (ds != null && ds.stacks() > 0) {
            int bonus = Math.round(damage * BONUS_MULT_PER_STACK * ds.stacks());
            if (bonus > 0) {
                defender.damage(bonus, attacker, DamageType.PHYSICAL);
            }
        }
        return damage;
    }
}
