/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EmberDamageReduction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 엠버 (Ember)
 *
 * 직군: 디펜더
 * 무기: 양손검
 * 속성: 열기
 *
 * ※ 카치르보다 방어 수치 낮음. 대신 딜 기여 높음.
 * ※ 넘어뜨리기 4스택 후 연계기 → 2연속 넉백 콤보 가능.
 *
 * [배틀스킬] 열기 피해(×SKILL_MULT) + 넘어뜨리기(KNOCKDOWN) + 1턴간 받는 피해 감소
 * [연계기]   조건: 적 차지 시작 or 넘어뜨리기 4스택 도달 시
 *             효과: 물리 피해(×CHAIN_MULT) + 넘어뜨리기 + Hero 소량 회복
 * [궁극기]   열기 피해(×ULT_MULT) + Hero 보호막(Barrier) 부여
 */
public class Ember extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 열기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.2f;

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /** 연계기 Hero 회복량 (최대HP 비율). TODO: 수치 확정 */
    private static final float CHAIN_HEAL_RATIO = 0.08f;

    /** 궁극기 열기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 2.0f;

    /** 궁극기 보호막량 (최대HP 비율). TODO: 수치 확정 */
    private static final float ULT_SHIELD_RATIO = 0.3f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "엠버"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.DEFENDER; }
    @Override public WeaponType weaponType()     { return WeaponType.TWO_HANDED_SWORD; }
    @Override public Attribute attribute()       { return Attribute.FIRE; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 열기 강타
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "열기 강타"; }
            @Override public String description() {
                return "열기 피해(×" + SKILL_MULT + ") + 넘어뜨리기 + 1턴간 피해 감소("
                        + (int)(EmberDamageReduction.REDUCTION * 100) + "%)";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 열기 피해
                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.HEAT);

                // 넘어뜨리기
                if (target.isAlive()) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.KNOCKDOWN, hero);
                }

                // 1턴간 피해 감소 버프
                Buff.affect(hero, EmberDamageReduction.class, EmberDamageReduction.DURATION);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 방패 격돌
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 4; } // TODO: 수치 확정

    @Override public String chainName()        { return "방패 격돌"; }
    @Override public String chainDescription() {
        return "조건: 적 차지 시작 or 넘어뜨리기 4스택 달성 시\n" +
               "효과: 물리 피해(×" + CHAIN_MULT + ") + 넘어뜨리기 + Hero 소량 회복";
    }

    /**
     * 연계기 조건:
     * 1) 적 차지 시작 (Charging 버프 보유) — Charging.startCharge() 시 checkChainTriggers 호출됨
     * 2) 넘어뜨리기 4스택 도달 — DefenselessStack.apply() 끝에서 checkChainTriggers 호출됨
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;

        // 차지 시작
        if (target.buff(Charging.class) != null) return true;

        // 넘어뜨리기 4스택 도달
        DefenselessStack ds = target.buff(DefenselessStack.class);
        return ds != null && ds.stacks() >= DefenselessStack.MAX_STACKS;
    }

    /**
     * 연계기 효과: 물리 피해 + 넘어뜨리기 + Hero 소량 회복.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 물리 피해
        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.PHYSICAL);

        // 넘어뜨리기 (차지→KNOCKDOWN → 2연속 넉백 콤보 가능)
        if (target.isAlive()) {
            DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.KNOCKDOWN, hero);
        }

        // Hero 소량 회복
        int heal = Math.round(hero.HT * CHAIN_HEAL_RATIO);
        hero.HP = Math.min(hero.HP + heal, hero.HT);
        hero.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
                "+" + heal);
    }

    // ─────────────────────────────────────────────
    // 궁극기: 불의 심판
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "불의 심판"; }
            @Override public String description() {
                return "열기 피해(×" + ULT_MULT + ") + Hero 보호막(최대HP×" + ULT_SHIELD_RATIO + ") 부여.";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 열기 피해
                int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(dmg, hero, DamageType.HEAT);

                // Hero 보호막
                int shield = Math.round(hero.HT * ULT_SHIELD_RATIO);
                Buff.affect(hero, Barrier.class).incShield(shield);
            }
        };
    }
}
