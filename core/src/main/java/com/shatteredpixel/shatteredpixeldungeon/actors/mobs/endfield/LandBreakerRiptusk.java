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
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SwarmSprite;
import com.watabou.utils.Random;

/**
 * 본크러셔 립터스크 (LandBreakerRiptusk) — 4번 협곡 1계층(A3~A4) 일반 몬스터.
 *
 * HP 15 / ATK 50 / 명중 12 / 회피 6 / 방어 2 all / EXP 3 / maxLvl 9
 * 이동속도 2배. 특수 패턴 없음. 공격속성: 물리.
 * 드랍: 비스트의 고기(50%, 현재 MysteryMeat 플레이스홀더)
 *
 * TODO: 전용 스프라이트, 수치 최종 확정, 전용 드랍 아이템(비스트의 고기) 구현
 */
public class LandBreakerRiptusk extends Mob {

    {
        spriteClass  = SwarmSprite.class; // TODO: 전용 스프라이트
        HP = HT      = 15;
        defenseSkill = 6;
        baseSpeed    = 2f; // 이동속도 2배
        EXP          = 3;
        maxLvl       = 9;

        lootChance   = 0f;
    }

    @Override
    public int damageRoll() {
        // ATK 50 기준. TODO: 최종 수치 확정
        return Random.NormalIntRange(2, 5);
    }

    @Override
    public int attackSkill(Char target) {
        return 12; // TODO: 최종 수치 확정
    }

    @Override
    public int drRoll() {
        // 방어 2 all. TODO: 속성별 방어 확장
        return super.drRoll() + Random.NormalIntRange(0, 2);
    }

    @Override
    public void die(Object cause) {
        super.die(cause);

        // 비스트의 고기 50% (TODO: BeastMeat 아이템 구현 후 교체)
        if (Random.Float() < 0.50f) {
            Dungeon.level.drop(new MysteryMeat(), pos).sprite.drop();
        }
    }
}
