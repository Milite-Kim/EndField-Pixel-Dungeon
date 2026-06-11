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
 *
 * ⚠️ 스텁: Hero에 공격 사거리(reach) 시스템이 아직 없어 효과가 적용되지 않는다.
 *     reach 시스템 구현 후 Hero가 reachBonus()를 참조하도록 연동 예정.
 *     docs/엔픽던_기질시스템.md §6 참조.
 *
 * TODO: reach 시스템 구현 + REACH_BONUS 수치 확정
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
