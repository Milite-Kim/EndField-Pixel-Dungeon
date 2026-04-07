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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
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
 * [배틀스킬] 섬광 베기 — 물리 피해(×SKILL_MULT) + 띄우기(LAUNCH)
 * [연계기]   관통 베기 — 조건: 방어불능 스택 보유 시
 *             효과: 물리 피해(×CHAIN_MULT) + LAUNCH
 *             추가: 진천우가 메인 오퍼레이터일 때 → 관통 이동 (적 뒤로 이동)
 * [궁극기]   절공 — 7회 연속 물리 피해(×ULT_HIT_MULT)
 *             TODO: 피해 수치 확정
 *             TODO: 승급 — 도중 처치 시 주변 적으로 연장 / 처치 비례 충전 반환
 */
public class Jincheonwoo extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해 배율 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 피해 배율 (hero.damageRoll() 기준). TODO: 수치 확정 */
    private static final float SKILL_MULT    = 1.5f;

    /** 연계기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT    = 1.2f;

    /** 궁극기 히트 1회당 피해 배율. TODO: 수치 확정 */
    private static final float ULT_HIT_MULT  = 0.8f;

    /** 궁극기 총 히트 수 */
    private static final int   ULT_HIT_COUNT = 7;

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
    // 배틀스킬: 섬광 베기
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 4; // TODO: 수치 확정
            }

            @Override
            public String name() { return "섬광 베기"; }

            @Override
            public String description() {
                return "물리 피해(" + SKILL_MULT + "×) + 띄우기(LAUNCH).\nTODO: 피해 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target) {
                if (target == null || !target.isAlive()) return;

                int damage = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                if (target.isAlive()) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 관통 베기
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    @Override
    public String chainName() { return "관통 베기"; }

    @Override
    public String chainDescription() {
        return "조건: 적 방어불능 스택 보유 시\n" +
               "효과: 물리 피해 + LAUNCH\n" +
               "진천우 메인 시 추가: 관통 이동";
    }

    /** 연계기 조건: 적에게 방어불능 스택이 있을 때 */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(DefenselessStack.class) != null;
    }

    /**
     * 연계기 효과: 물리 피해 + LAUNCH.
     * 진천우가 메인 오퍼레이터일 때만 관통 이동(적 뒤로 이동)을 수행한다.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 물리 피해
        int damage = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(damage, hero, DamageType.PHYSICAL);

        // 띄우기
        if (target.isAlive()) {
            DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
        }

        // 관통 이동: 진천우가 메인 오퍼레이터일 때만
        if (hero.activeMainOperator instanceof Jincheonwoo) {
            penetrate(hero, target);
        }
    }

    /**
     * 관통 이동: 영웅이 적을 통과해 적 뒤쪽 1칸으로 이동한다.
     * 목표 셀이 이동 불가하거나 다른 캐릭터가 있으면 이동하지 않는다.
     *
     * 방향: hero → enemy → (enemy + delta)
     */
    private void penetrate(Hero hero, Char target) {
        int width = Dungeon.level.width();
        int dx = Integer.signum((target.pos % width) - (hero.pos % width));
        int dy = Integer.signum((target.pos / width) - (hero.pos / width));

        if (dx == 0 && dy == 0) return;

        int destCell = target.pos + dy * width + dx;
        if (destCell < 0 || destCell >= Dungeon.level.length()) return;
        if (!Dungeon.level.passable[destCell]) return;
        if (Actor.findChar(destCell) != null) return;

        hero.sprite.move(hero.pos, destCell);
        hero.move(destCell);
    }

    // ─────────────────────────────────────────────
    // 궁극기: 절공
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            public String name() { return "절공"; }

            @Override
            public String description() {
                return ULT_HIT_COUNT + "회 연속 물리 피해(" + ULT_HIT_MULT + "×/회).\n" +
                       "TODO: 피해 수치 확정\n" +
                       "TODO(승급): 도중 처치 시 주변 적으로 연장 / 처치 비례 충전 반환";
            }

            @Override
            protected void activate(Hero hero, Char target) {
                if (target == null || !target.isAlive()) return;

                // 7회 연속 물리 피해
                // TODO: 히트 간 애니메이션 시퀀스 (현재는 즉시 계산)
                for (int i = 0; i < ULT_HIT_COUNT; i++) {
                    if (!target.isAlive()) break;
                    int damage = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                    target.damage(damage, hero, DamageType.PHYSICAL);
                }
            }
        };
    }
}
