/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Electrified;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 펠리카 (Felika)
 *
 * 직군: 캐스터
 * 무기: 아츠유닛
 * 속성: 전기
 *
 * [배틀스킬] 전기 피해(×SKILL_MULT) + 전기 부착
 *             기존 충전 소모 → 소모량(칸) 비례 순간이동
 *             이후 아츠유닛 충전 +1 (최대 3)
 *
 * [충전 효과] 충전량 비례 순간이동 (충전 1당 1칸, 최대 3칸)
 *             → 배틀스킬 사용 시 기존 충전 소모 → Hero가 주변 임의 셀로 이동
 *
 * [연계기]   조건: 강력한 일격 적중 시 (finishingBlowContext = true)
 *             효과: 전기 피해(×CHAIN_MULT) + 감전(Electrified) 부여
 *
 * [궁극기]   대량 전기 피해(×ULT_MULT). 충전 요구량 낮음.
 *
 * ※ 첫 번째 던전 시작 시 팀 오퍼레이터 기본 보유 예정
 */
public class Felika extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 전기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.0f;

    /** 연계기 전기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.9f;

    /** 연계기 감전 스택. TODO: 수치 확정 */
    private static final int CHAIN_ELEC_STACKS = 2;

    /** 궁극기 전기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 2.0f;

    // ─────────────────────────────────────────────
    // 아츠유닛 충전 (배틀스킬 시 +1, 최대 3)
    // ─────────────────────────────────────────────

    /** 최대 충전량. */
    private static final int MAX_CHARGES = 3;

    /** 현재 충전량. */
    private int charges = 0;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "펠리카"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.CASTER; }
    @Override public WeaponType weaponType()     { return WeaponType.ARTS_UNIT; }
    @Override public Attribute attribute()       { return Attribute.ELECTRIC; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 전기 충격
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "전기 충격"; }
            @Override public String description() {
                return "전기 피해(×" + SKILL_MULT + ") + 전기 부착.\n" +
                       "충전 소모 → 소모량 칸 수 만큼 주변 임의 위치로 순간이동.\n" +
                       "이후 충전 +1 (최대 " + MAX_CHARGES + ").";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 기존 충전 소모 → 순간이동
                if (charges > 0) {
                    teleport(hero, charges);
                    charges = 0;
                }

                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.ELECTRIC);

                if (target.isAlive()) {
                    ArtsAttachment.apply(target, ArtsAttachment.ArtsType.ELECTRIC, hero);
                }

                // 아츠유닛 충전 +1
                if (charges < MAX_CHARGES) charges++;
            }
        };
    }

    /**
     * Hero를 현재 위치로부터 BFS 거리 [1, range] 이내의 임의 통행 가능 빈 셀로 이동.
     * 이동 가능한 셀이 없으면 현재 위치 유지.
     */
    private void teleport(Hero hero, int range) {
        PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable, range);
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < Dungeon.level.length(); i++) {
            int d = PathFinder.distance[i];
            if (d >= 1 && d <= range
                    && Dungeon.level.passable[i]
                    && Actor.findChar(i) == null) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return;

        int dest = Random.element(candidates);
        hero.sprite.move(hero.pos, dest);
        hero.move(dest, false);
        Dungeon.observe();
    }

    // ─────────────────────────────────────────────
    // 연계기: 과부하
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "과부하"; }
    @Override public String chainDescription() {
        return "조건: 강력한 일격 적중 시\n" +
               "효과: 전기 피해(×" + CHAIN_MULT + ") + 감전";
    }

    /**
     * 연계기 조건: 강력한 일격(피니싱 블로우) 적중 직후.
     * Hero.onFinishingBlowLanded() → finishingBlowContext = true.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        return hero.finishingBlowContext && target != null && target.isAlive();
    }

    /** 연계기 효과: 전기 피해 + 감전 부여 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.ELECTRIC);

        if (target.isAlive()) {
            Electrified.apply(target, CHAIN_ELEC_STACKS);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 초전도 방전
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            // 충전 요구량이 낮아 자주 사용 가능
            @Override public int maxCharge() { return 70; } // TODO: 수치 확정
            @Override public String name()   { return "초전도 방전"; }
            @Override public String description() {
                return "대량 전기 피해(×" + ULT_MULT + ").\n" +
                       "충전 요구량이 낮아 자주 사용 가능.";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(dmg, hero, DamageType.ELECTRIC);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기 (charges)
    // ─────────────────────────────────────────────

    private static final String CHARGES = "charges";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CHARGES, charges);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        charges = bundle.getInt(CHARGES);
    }
}
