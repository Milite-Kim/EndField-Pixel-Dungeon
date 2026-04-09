/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combustion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Electrified;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 울프가드 (Wolfguard)
 *
 * 직군: 캐스터
 * 무기: 권총
 * 속성: 열기
 *
 * [배틀스킬] 열기 피해(×SKILL_MULT) + 열기 부착.
 *             연소(Combustion) 상태 → 소모 → 추가 대량 열기 피해(×SKILL_CONSUME_MULT)
 *             감전(Electrified) 상태 → 소모 → 추가 대량 열기 피해(×SKILL_CONSUME_MULT)
 *             (연소·감전 동시 보유 시 각각 소모 → 각각 피해 — 현재는 단일 적용)
 * [연계기]   조건: 적에게 아츠 부착 부여 시 (ArtsAttachment.apply() 내부에서 checkChainTriggers 호출)
 *             효과: 열기 피해 + 열기 부착
 * [궁극기]   열기 피해(×ULT_MULT) + 강제 연소(Combustion, 3스택 기준)
 */
public class Wolfguard extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 기본 열기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.0f;

    /** 배틀스킬 연소/감전 소모 시 추가 열기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_CONSUME_MULT = 1.5f;

    /** 연계기 열기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.8f;

    /** 궁극기 열기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 2.5f;

    /** 궁극기 강제 연소 스택 기준. TODO: 수치 확정 */
    private static final int ULT_COMBUSTION_STACKS = 3;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "울프가드"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.CASTER; }
    @Override public WeaponType weaponType()     { return WeaponType.HANDGUN; }
    @Override public Attribute attribute()       { return Attribute.FIRE; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 발화 사격
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "발화 사격"; }
            @Override public String description() {
                return "열기 피해(×" + SKILL_MULT + ") + 열기 부착.\n" +
                       "연소 or 감전 상태 → 소모 → 추가 대량 열기 피해(×" + SKILL_CONSUME_MULT + ")";
            }

            @Override public int range() { return 5; } // 권총

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 기본 열기 피해
                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.HEAT);

                if (!target.isAlive()) return;

                // 열기 부착
                ArtsAttachment.apply(target, ArtsAttachment.ArtsType.HEAT, hero);

                if (!target.isAlive()) return;

                // 연소 소모 → 추가 대량 열기 피해
                Combustion combustion = target.buff(Combustion.class);
                if (combustion != null) {
                    combustion.detach();
                    int bonusDmg = Math.round(hero.damageRoll() * SKILL_CONSUME_MULT);
                    target.damage(bonusDmg, hero, DamageType.HEAT);
                }

                if (!target.isAlive()) return;

                // 감전 소모 → 추가 대량 열기 피해
                Electrified electrified = target.buff(Electrified.class);
                if (electrified != null) {
                    electrified.detach();
                    int bonusDmg = Math.round(hero.damageRoll() * SKILL_CONSUME_MULT);
                    target.damage(bonusDmg, hero, DamageType.HEAT);
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 추적 사격
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "추적 사격"; }
    @Override public String chainDescription() {
        return "조건: 적에게 아츠 부착 부여 시 (종류 무관)\n" +
               "효과: 열기 피해(×" + CHAIN_MULT + ") + 열기 부착";
    }

    /**
     * 연계기 조건: 적이 아츠 부착을 보유 중일 때.
     * ArtsAttachment.apply() → checkChainTriggers() 호출 시 이 조건이 true가 됨.
     * (부착 직후 상태이므로 부착 행위와 실질적으로 동기화)
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(ArtsAttachment.class) != null;
    }

    /**
     * 연계기 효과: 열기 피해 + 열기 부착.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.HEAT);

        if (target.isAlive()) {
            ArtsAttachment.apply(target, ArtsAttachment.ArtsType.HEAT, hero);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 불꽃 사냥
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "불꽃 사냥"; }
            @Override public String description() {
                return "열기 피해(×" + ULT_MULT + ") + 강제 연소(" + ULT_COMBUSTION_STACKS + "스택 기준).";
            }

            @Override public int range() { return 5; } // 권총

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(dmg, hero, DamageType.HEAT);

                if (target.isAlive()) {
                    Combustion.apply(target, ULT_COMBUSTION_STACKS);
                }
            }
        };
    }
}
