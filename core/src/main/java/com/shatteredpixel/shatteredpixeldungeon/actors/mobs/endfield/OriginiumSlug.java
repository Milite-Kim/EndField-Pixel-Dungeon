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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RatSprite;
import com.watabou.utils.Random;

/**
 * 원석충 (OriginiumSlug) — 4번 협곡 1계층(A1~A4) 일반 몬스터.
 *
 * HP 8 / ATK 40 / 명중 8 / 회피 2 / 방어 1 all / EXP 2 / maxLvl 5
 * 특수 패턴 없음. 공격속성: 물리.
 *
 * TODO: 전용 스프라이트, 수치 최종 확정
 */
public class OriginiumSlug extends Mob {

    {
        spriteClass = RatSprite.class; // TODO: 전용 스프라이트
        HP = HT      = 8;
        defenseSkill = 2;
        EXP          = 2;
        maxLvl       = 5;

        // 드랍 없음
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
        // 물리 방어 1. TODO: 속성별 방어 시스템 추가 시 확장
        return super.drRoll() + Random.NormalIntRange(0, 1);
    }
}
