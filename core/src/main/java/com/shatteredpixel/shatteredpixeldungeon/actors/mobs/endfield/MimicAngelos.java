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
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BatSprite;
import com.watabou.utils.Random;

/**
 * 모방아겔로스 (MimicAngelos) — 4번 협곡 1계층(A2~A4) 일반 몬스터.
 *
 * HP 30 / ATK 40 / 명중 8 / 회피 5 / 방어 0 / EXP 3 / maxLvl 9
 * 비행 유닛. 특수 패턴 없음. 공격속성: 물리.
 * 드랍: 메밀꽃 약재(33%, 현재 SmallRation 플레이스홀더)
 *
 * TODO: 전용 스프라이트, 수치 최종 확정, 전용 드랍 아이템(메밀꽃 약재) 구현
 */
public class MimicAngelos extends Mob {

    {
        spriteClass  = BatSprite.class; // TODO: 전용 스프라이트
        HP = HT      = 30;
        defenseSkill = 5;
        EXP          = 3;
        maxLvl       = 9;

        flying       = true;

        lootChance   = 0f;
    }

    @Override
    public int damageRoll() {
        // ATK 40 기준. TODO: 최종 수치 확정
        return Random.NormalIntRange(2, 4);
    }

    @Override
    public int attackSkill(Char target) {
        return 8; // TODO: 최종 수치 확정
    }

    @Override
    public int drRoll() {
        // 방어 0
        return super.drRoll();
    }

    @Override
    public void die(Object cause) {
        flying = false;
        super.die(cause);

        // 메밀꽃 약재 33% (TODO: BuckwheatHerb 아이템 구현 후 교체)
        if (Random.Float() < 0.33f) {
            Dungeon.level.drop(new SmallRation(), pos).sprite.drop();
        }
    }
}
