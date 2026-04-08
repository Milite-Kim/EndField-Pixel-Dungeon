/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frozen;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.OriginiumCrystal;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.utils.Random;

/**
 * 알레쉬 (Alesh)
 *
 * 직군: 뱅가드
 * 무기: 한손검
 * 속성: 냉기
 *
 * [배틀스킬] 물리 피해(×SKILL_MULT).
 *             냉기 부착 보유 적 → 전량 소모 → 강제 동결 + 소모 스택 비례 궁극기 충전
 * [연계기]   조건: 적이 아츠 상태이상 보유 OR 오리지늄 아츠 결정이 소모될 때
 *             효과: 물리 피해 + 궁극기 충전 + 일정 확률 강화 피해
 * [궁극기]   대량 냉기 피해(×ULT_MULT) + 냉기 부착
 */
public class Alesh extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 물리 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.2f;

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /** 연계기 강화 피해 발동 확률. TODO: 수치 확정 */
    private static final float CHAIN_ENHANCE_CHANCE = 0.30f;

    /** 연계기 강화 피해 배율 (추가). TODO: 수치 확정 */
    private static final float CHAIN_ENHANCE_MULT = 0.5f;

    /** 연계기 궁극기 충전량. TODO: 수치 확정 */
    private static final int CHAIN_CHARGE = 15;

    /** 궁극기 냉기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 2.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "알레쉬"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.VANGUARD; }
    @Override public WeaponType weaponType()     { return WeaponType.ONE_HANDED_SWORD; }
    @Override public Attribute attribute()       { return Attribute.COLD; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 빙결 참
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown() { return 3; } // TODO: 수치 확정 (뱅가드: 짧은 쿨타임)
            @Override public String name()        { return "빙결 참"; }
            @Override public String description() {
                return "물리 피해(×" + SKILL_MULT + ").\n" +
                       "냉기 부착 보유 시: 전량 소모 → 강제 동결 + 소모 스택 비례 궁극기 충전";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 기본 물리 피해
                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.PHYSICAL);

                if (!target.isAlive()) return;

                // 냉기 부착 소모 → 강제 동결 + 궁극기 충전
                ArtsAttachment arts = target.buff(ArtsAttachment.class);
                if (arts != null && arts.currentType() == ArtsAttachment.ArtsType.CRYO) {
                    int consumedStacks = arts.stacks();
                    arts.detach();

                    // 강제 동결
                    Frozen.apply(target, consumedStacks);

                    // 소모 스택 비례 궁극기 충전
                    if (hero.activeUltimate != null) {
                        hero.activeUltimate.addChargeInternal(consumedStacks * 8); // TODO: 수치 확정
                    }
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 빙격
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "빙격"; }
    @Override public String chainDescription() {
        return "조건: 적 아츠 상태이상 보유 OR 오리지늄 아츠 결정 소모 시\n" +
               "효과: 물리 피해 + 궁극기 충전 + " + (int)(CHAIN_ENHANCE_CHANCE*100) + "% 확률 강화 피해";
    }

    /**
     * 연계기 조건: 적이 아츠 부착(ArtsAttachment) 보유 OR 오리지늄 아츠 결정(OriginiumCrystal) 소모됨.
     * 결정 소모 트리거는 DefenselessStack.apply()의 훅이 checkChainTriggers를 호출할 때 감지.
     * 단순히 아츠 상태이상 여부만 체크 (소모 시점은 시스템이 보장).
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(ArtsAttachment.class) != null
                || target.buff(OriginiumCrystal.class) != null;
    }

    /** 연계기 효과: 물리 피해 + 궁극기 충전 + 확률 강화 피해 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.PHYSICAL);

        // 궁극기 충전
        if (hero.activeUltimate != null) {
            hero.activeUltimate.addChargeInternal(CHAIN_CHARGE);
        }

        // 확률 강화 피해
        if (target.isAlive() && Random.Float() < CHAIN_ENHANCE_CHANCE) {
            int bonusDmg = Math.round(hero.damageRoll() * CHAIN_ENHANCE_MULT);
            target.damage(bonusDmg, hero, DamageType.PHYSICAL);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 빙하 돌격
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "빙하 돌격"; }
            @Override public String description() {
                return "대량 냉기 피해(×" + ULT_MULT + ") + 냉기 부착.";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(dmg, hero, DamageType.COLD);

                if (target.isAlive()) {
                    ArtsAttachment.apply(target, ArtsAttachment.ArtsType.CRYO, hero);
                }
            }
        };
    }
}
