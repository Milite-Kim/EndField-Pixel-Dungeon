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
import com.shatteredpixel.shatteredpixeldungeon.items.traits.common.GritTrait;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.common.KineticTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BruteSprite;
import com.watabou.utils.Random;

/**
 * 본크러셔 약탈자 (LandBreakerPlunderer) — 4번 협곡 1계층(A3~A4) 일반 몬스터.
 *
 * HP 25 / ATK 60 / 명중 12 / 회피 5 / 방어 3 all / EXP 3 / maxLvl 9
 * 특수 패턴 없음. 공격속성: 물리.
 * 드랍: 1~2티어 기질 랜덤(25%, GritTrait 또는 KineticTrait 플레이스홀더)
 *
 * TODO: 전용 스프라이트, 수치 최종 확정, 기질 아이템 최종 확정
 */
public class LandBreakerPlunderer extends Mob {

    {
        spriteClass  = BruteSprite.class; // TODO: 전용 스프라이트
        HP = HT      = 25;
        defenseSkill = 5;
        EXP          = 3;
        maxLvl       = 9;

        lootChance   = 0f;
    }

    @Override
    public int damageRoll() {
        // ATK 60 기준. TODO: 최종 수치 확정
        return Random.NormalIntRange(3, 6);
    }

    @Override
    public int attackSkill(Char target) {
        return 12; // TODO: 최종 수치 확정
    }

    @Override
    public int drRoll() {
        // 방어 3 all. TODO: 속성별 방어 확장
        return super.drRoll() + Random.NormalIntRange(0, 3);
    }

    @Override
    public void die(Object cause) {
        super.die(cause);

        // 1~2티어 기질 25% 랜덤 드랍 (TODO: 최종 기질 아이템 확정 후 교체)
        if (Random.Float() < 0.25f) {
            if (Random.Int(2) == 0) {
                Dungeon.level.drop(new GritTrait(), pos).sprite.drop();
            } else {
                Dungeon.level.drop(new KineticTrait(), pos).sprite.drop();
            }
        }
    }
}
