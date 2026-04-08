/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 판 (Pan)
 *
 * 직군: 스트라이커
 * 무기: 양손검
 * 속성: 물리
 *
 * [배틀스킬] 물리 피해(×SKILL_MULT) + 띄우기(LAUNCH)
 * [연계기]   조건: 적 방어불능 스택 4스택(MAX_STACKS) 시
 *             효과: 물리 피해(×CHAIN_MULT) + 강타(CHAIN_HEAVY_MULT — 기본 강타보다 10% 높음)
 * [궁극기]   대량 물리 피해(×ULT_MULT) + 띄우기(LAUNCH) → 넘어뜨리기(KNOCKDOWN)
 *
 * ※ 포그라니치니크/관리자와 파티 구성 시 스택 유지 어려움 주의
 *    (연계기가 4스택을 강타로 전량 소모하므로 스택 재축적 전 다른 오퍼레이터가 소모하면 연계기 미발동)
 */
public class Pan extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해 배율 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.2f;

    /** 연계기 선타 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /**
     * 연계기 강타 배율: 기본 강타(DefenselessStack.HEAVY_ATTACK_DMG_MULT)보다 10% 높음.
     * 추후 기본 배율이 변경되어도 자동으로 연동된다.
     */
    private static final float CHAIN_HEAVY_MULT = DefenselessStack.HEAVY_ATTACK_DMG_MULT * 1.10f;

    /** 궁극기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 2.0f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "판"; }

    @Override
    public OperatorClass operatorClass() { return OperatorClass.STRIKER; }

    @Override
    public WeaponType weaponType() { return WeaponType.TWO_HANDED_SWORD; }

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
            public String name() { return "강습 베기"; } // TODO: 정식 스킬명 확정

            @Override
            public String description() {
                return "물리 피해(" + SKILL_MULT + "×) + 띄우기. TODO: 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
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
    // 연계기: 물리 피해 + 강타 (고배율)
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    @Override
    public String chainName() { return "폭발 강타"; } // TODO: 정식 스킬명 확정

    @Override
    public String chainDescription() {
        return "조건: 방어불능 4스택 시\n" +
               "물리 피해(" + CHAIN_MULT + "×) + 강타(배율 " + CHAIN_HEAVY_MULT + "×/스택, 기본 대비 +10%)";
    }

    /**
     * 연계기 조건: 적 방어불능 스택이 최대(4스택)에 도달했을 때.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        DefenselessStack ds = target.buff(DefenselessStack.class);
        return ds != null && ds.stacks() >= DefenselessStack.MAX_STACKS;
    }

    /**
     * 연계기 효과: 물리 피해 + 강타(기본 배율 +10%).
     * CHAIN_HEAVY_MULT를 apply() 4-param 오버로드에 전달하여 보정 배율로 강타를 발동.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 선타 물리 피해
        int damage = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(damage, hero, DamageType.PHYSICAL);

        // 강타: 기본 배율보다 10% 높은 배율로 스택 전량 소모
        if (target.isAlive()) {
            DefenselessStack.apply(
                    target,
                    DefenselessStack.PhysicalAbnormality.HEAVY_ATTACK,
                    hero,
                    CHAIN_HEAVY_MULT);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 대량 물리 피해 + 띄우기 → 넘어뜨리기
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            public String name() { return "천지파쇄"; } // TODO: 정식 스킬명 확정

            @Override
            public String description() {
                return "대량 물리 피해(" + ULT_MULT + "×) + 띄우기 → 넘어뜨리기. TODO: 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 대량 물리 피해
                int damage = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                // 띄우기 → 넘어뜨리기 순서로 적용 (스택 축적 + 연속 이상)
                if (target.isAlive()) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
                }
                if (target.isAlive()) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.KNOCKDOWN, hero);
                }
            }
        };
    }
}
