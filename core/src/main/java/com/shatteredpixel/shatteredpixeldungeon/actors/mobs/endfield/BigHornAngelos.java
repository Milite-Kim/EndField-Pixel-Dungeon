/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.endfield;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.common.GritTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GnollSprite;
import com.watabou.utils.Random;

/**
 * 큰뿔아겔로스 (BigHornAngelos) — 4번 협곡 1계층(A2~A4) 일반 몬스터.
 *
 * HP 12 / ATK 60 / 명중 10 / 회피 4 / 방어 2 all / EXP 2 / maxLvl 8
 * 특수 패턴 없음. 공격속성: 물리.
 * 드랍: 혼탁기질(10%, GritTrait 플레이스홀더) + 크레디트(50%)
 *
 * TODO: 전용 스프라이트, 수치 최종 확정, 혼탁기질 아이템 구현
 */
public class BigHornAngelos extends Mob {

    {
        spriteClass  = GnollSprite.class; // TODO: 전용 스프라이트
        HP = HT      = 12;
        defenseSkill = 4;
        EXP          = 2;
        maxLvl       = 8;

        lootChance   = 0f;
    }

    @Override
    public int damageRoll() {
        // ATK 60 기준. TODO: 최종 수치 확정
        return Random.NormalIntRange(3, 6);
    }

    @Override
    public int attackSkill(Char target) {
        return 10; // TODO: 최종 수치 확정
    }

    @Override
    public int drRoll() {
        // 방어 2 all. TODO: 속성별 방어 확장
        return super.drRoll() + Random.NormalIntRange(0, 2);
    }

    @Override
    public void die(Object cause) {
        super.die(cause);

        // 혼탁기질 10% (TODO: TurbidTrait 구현 후 교체)
        if (Random.Float() < 0.10f) {
            Dungeon.level.drop(new GritTrait(), pos).sprite.drop();
        }
        // 크레디트 50%
        if (Random.Float() < 0.50f) {
            Dungeon.level.drop(new Gold(Random.NormalIntRange(1, 5)), pos).sprite.drop();
        }
    }
}
