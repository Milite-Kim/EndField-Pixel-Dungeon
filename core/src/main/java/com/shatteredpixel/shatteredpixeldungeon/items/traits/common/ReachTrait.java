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
 * 방출 (ReachTrait) — 공용 수식어 '방출' 기질.
 *
 * 공격 사거리를 REACH_BONUS 칸 증가시킨다.
 * Hero.operatorReach()가 belongings.trait.reachBonus()를 합산한다.
 *
 * TODO: REACH_BONUS 수치 확정
 */
public class ReachTrait extends CommonTrait {

    private static final int REACH_BONUS = 1; // TODO: 수치 확정

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public ReachTrait()         { super(1); }
    public ReachTrait(int tier) { super(tier); }

    @Override
    public int reachBonus() {
        return REACH_BONUS;
    }
}
