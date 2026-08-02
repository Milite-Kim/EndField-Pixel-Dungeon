/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.items.traits.common;

import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 혼탁 (TurbidTrait) — 1티어 기본 기질.
 *
 * 수식어(강공·의료 등) 없이 티어 기본 ATK만 제공하는 가장 기본적인 기질.
 * 오퍼레이터는 게임 시작 시 이 기질을 장착한 상태로 출발한다.
 *
 * ATK는 {@link CommonTrait}의 티어 테이블에서 온다 (1티어 = +50).
 * 기질이 ATK의 주축이므로, 이것이 없으면 기본 공격이 사실상 무력화된다.
 */
public class TurbidTrait extends CommonTrait {

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public TurbidTrait() { super(1); }
}
