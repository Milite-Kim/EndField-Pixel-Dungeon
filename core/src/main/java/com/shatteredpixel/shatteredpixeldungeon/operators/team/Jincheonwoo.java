/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 진천우 (Jincheonwoo)
 *
 * 직군: 가드
 * 무기: 한손검
 * 속성: 물리
 *
 * [배틀스킬] 물리 피해 + 띄우기 (LAUNCH)
 * [연계기]
 *   - 조건: 적 방어불능 스택 누적 시
 *   - 효과: 관통 베기 → 물리 피해 + 띄우기 (관통 이동 가능)
 * [궁극기] 7회 연속 대량 물리 피해
 *   ※ 승급 확장: 궁극기 도중 적 처치 시 주변 적으로 연장 / 처치 비례 충전 반환
 */
public class Jincheonwoo extends TeamOperator {

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "진천우"; }

    @Override
    public OperatorClass operatorClass() { return OperatorClass.GUARD; }

    @Override
    public WeaponType weaponType() { return WeaponType.ONE_HANDED_SWORD; }

    @Override
    public Attribute attribute() { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 물리 피해 + 띄우기
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 4; // TODO: 수치 확정
            }

            @Override
            protected void activate(Hero hero, Char target) {
                if (target == null || !target.isAlive()) return;

                // TODO: 물리 피해 처리 (피해 수치 미확정)
                // target.damage(calculateDamage(hero), this);

                // 띄우기 적용 (DefenselessStack 시스템 연동)
                DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기 (TeamOperator 구현)
    // ─────────────────────────────────────────────

    /** 연계기 기본 쿨타임 */
    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    /**
     * 연계기 조건: 적에게 방어불능 스택이 누적되어 있을 때
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(DefenselessStack.class) != null;
    }

    /**
     * 연계기 효과: 관통 베기 → 물리 피해 + 띄우기 (관통 이동 가능)
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // TODO: 물리 피해 처리 (피해 수치 미확정)
        // TODO: 관통 이동 처리 (이동 시스템 연동 후 구현)

        // 띄우기 적용
        DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH);

        resetCooldown();
    }

    // ─────────────────────────────────────────────
    // 궁극기: 7회 연속 대량 물리 피해
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            protected void activate(Hero hero, Char target) {
                if (target == null || !target.isAlive()) return;

                // TODO: 7회 연속 물리 피해 처리 (히트 시퀀스 시스템 완성 후)
                // TODO: 승급 확장 - 궁극기 도중 적 처치 시 주변 적으로 연장 / 처치 비례 충전 반환
            }
        };
    }
}
