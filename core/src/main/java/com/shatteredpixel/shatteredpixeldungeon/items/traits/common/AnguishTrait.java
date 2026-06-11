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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AnguishBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.CommonTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 고통 (AnguishTrait) — 공용 수식어 '고통' 기질.
 *
 * 궁극기 사용 직후 일정 턴 동안 공격력이 증가하는 결전 준비형 효과.
 * Ultimate.use() → onUltimateUsed()에서 AnguishBuff를 부여하고,
 * Hero.getATK()가 해당 버프를 참조해 ATK를 증가시킨다.
 *
 * TODO: AnguishBuff.DURATION / ATK_MULT 수치 확정
 */
public class AnguishTrait extends CommonTrait {

    {
        image = ItemSpriteSheet.SOMETHING; // TODO: 전용 스프라이트
    }

    public AnguishTrait()         { super(1); }
    public AnguishTrait(int tier) { super(tier); }

    @Override
    public void onUltimateUsed(Hero hero) {
        Buff.affect(hero, AnguishBuff.class, AnguishBuff.DURATION);
    }
}
