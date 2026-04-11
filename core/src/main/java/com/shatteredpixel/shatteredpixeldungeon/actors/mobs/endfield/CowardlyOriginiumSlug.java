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
import com.shatteredpixel.shatteredpixeldungeon.items.food.Berry;
import com.shatteredpixel.shatteredpixeldungeon.items.traits.common.GritTrait;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SnakeSprite;
import com.watabou.utils.Random;

/**
 * 겁쟁이 원석충 (CowardlyOriginiumSlug) — 4번 협곡 1계층(A1~A4) 일반 몬스터.
 *
 * HP 1 / ATK 20 / 명중 8 / 회피 35 / 방어 0 / EXP 2 / maxLvl 6
 * 회피 35로 인해 기습·확정 명중 외에는 타격이 거의 불가능.
 * 드랍: 혼탁기질(5%, 현재 GritTrait 플레이스홀더) + 작물(50%, 현재 Berry 플레이스홀더)
 *
 * TODO: 전용 스프라이트, 수치 최종 확정, 전용 드랍 아이템(혼탁기질·작물) 구현
 */
public class CowardlyOriginiumSlug extends Mob {

    {
        spriteClass  = SnakeSprite.class; // TODO: 전용 스프라이트
        HP = HT      = 1;
        defenseSkill = 35; // 상시 회피 +35
        EXP          = 2;
        maxLvl       = 6;

        // 다중 드랍은 die()에서 직접 처리
        lootChance   = 0f;
    }

    @Override
    public int damageRoll() {
        // ATK 20 기준. TODO: 최종 수치 확정
        return Random.NormalIntRange(1, 2);
    }

    @Override
    public int attackSkill(Char target) {
        return 8; // TODO: 최종 수치 확정
    }

    @Override
    public int drRoll() {
        // 방어 0 — 원석충보다 약한 몸
        return super.drRoll();
    }

    @Override
    public void die(Object cause) {
        super.die(cause);

        // 혼탁기질 5% (TODO: TurbidTrait 구현 후 교체)
        if (Random.Float() < 0.05f) {
            Dungeon.level.drop(new GritTrait(), pos).sprite.drop();
        }
        // 작물 50% (TODO: Crop 아이템 구현 후 교체)
        if (Random.Float() < 0.50f) {
            Dungeon.level.drop(new Berry(), pos).sprite.drop();
        }
    }
}
