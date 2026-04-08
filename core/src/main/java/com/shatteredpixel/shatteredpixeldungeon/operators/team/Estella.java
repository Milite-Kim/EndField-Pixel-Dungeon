/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frozen;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalVulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 에스텔라 (Estella)
 *
 * 직군: 가드
 * 무기: 장병기
 * 속성: 냉기
 *
 * [배틀스킬] 냉기 피해(×SKILL_MULT) + 냉기 부착
 * [연계기]   조건: 적 동결 상태 시
 *             효과: 물리 피해(×CHAIN_MULT) + 띄우기(LAUNCH)
 *                   → 동결 적 적중 시 쇄빙 발동: 동결 소모 + 추가 물리 피해(×CHAIN_SHATTER_MULT) + 물리 취약
 *             ※ DefenselessStack.apply(LAUNCH)으로 쇄빙 자동 발동 (Frozen 버프 체크)
 * [궁극기]   물리 피해(×ULT_MULT). 물리 취약 상태 시 추가 띄우기(LAUNCH)
 *
 * ※ 동결→물리 변환 브릿지 역할
 */
public class Estella extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 냉기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.0f;

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /** 연계기 쇄빙 추가 피해 배율 (동결 적 적중 시). TODO: 수치 확정 */
    private static final float CHAIN_SHATTER_MULT = 0.8f;

    /** 궁극기 물리 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 1.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "에스텔라"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.GUARD; }
    @Override public WeaponType weaponType()     { return WeaponType.POLEARM; }
    @Override public Attribute attribute()       { return Attribute.COLD; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 냉기 찌르기
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown() { return 4; } // TODO: 수치 확정
            @Override public String name()        { return "냉기 찌르기"; }
            @Override public String description() {
                return "냉기 피해(×" + SKILL_MULT + ") + 냉기 부착.";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.COLD);

                if (target.isAlive()) {
                    ArtsAttachment.apply(target, ArtsAttachment.ArtsType.CRYO, hero);
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 쇄빙 타격
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 4; } // TODO: 수치 확정

    @Override public String chainName()        { return "쇄빙 타격"; }
    @Override public String chainDescription() {
        return "조건: 적 동결 상태 시\n" +
               "효과: 물리 피해 + 띄우기. 동결 소모 → 추가 물리 피해 + 물리 취약";
    }

    /**
     * 연계기 조건: 적이 동결(Frozen) 상태 시.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        Frozen frozen = target.buff(Frozen.class);
        return frozen != null && frozen.isFrozen();
    }

    /**
     * 연계기 효과: 물리 피해 + LAUNCH.
     * DefenselessStack.apply(LAUNCH)이 내부에서 Frozen 체크 → 쇄빙 발동 (동결 소모 + 쇄빙 피해).
     * 그 후 PhysicalVulnerable 적용.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        boolean hadFrozen = target.buff(Frozen.class) != null;

        // 물리 피해
        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.PHYSICAL);

        if (!target.isAlive()) return;

        // 띄우기 (DefenselessStack 내부에서 Frozen 체크 → 쇄빙 자동 발동)
        DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);

        if (!target.isAlive()) return;

        // 동결 상태였다면: 추가 물리 피해 + 물리 취약
        if (hadFrozen) {
            int shatterDmg = Math.round(hero.damageRoll() * CHAIN_SHATTER_MULT);
            target.damage(shatterDmg, hero, DamageType.PHYSICAL);

            if (target.isAlive()) {
                PhysicalVulnerable.apply(target);
            }
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 관통 찌르기
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "관통 찌르기"; }
            @Override public String description() {
                return "물리 피해(×" + ULT_MULT + "). 물리 취약 상태 시 추가 띄우기.";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                boolean hasPhysVuln = target.buff(PhysicalVulnerable.class) != null;

                int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(dmg, hero, DamageType.PHYSICAL);

                // 물리 취약 시 추가 LAUNCH
                if (target.isAlive() && hasPhysVuln) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
                }
            }
        };
    }
}
