/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AntalAmplificationBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElectricVulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeatVulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.utils.Bundle;

/**
 * 안탈 (Antal)
 *
 * 직군: 서포터
 * 무기: 아츠유닛
 * 속성: 전기
 *
 * [배틀스킬] 전기 피해(×SKILL_MULT)
 *             + 전기 취약(ElectricVulnerable) + 열기 취약(HeatVulnerable) 동시 부여
 *             아츠유닛 배틀스킬 시 충전 +1 (최대 3)
 *
 * [충전 효과] 충전량 비례 시간동안 물리 피해 감소
 *             배틀스킬 사용 시 기존 충전 소모 → 물리 피해 감소 버프 적용 후 +1 충전
 *             TODO: 물리 피해 감소 버프 클래스 구현 후 연동
 *
 * [연계기]   조건: 대상에게 물리 이상(DefenselessStack.apply) or 아츠 부착(ArtsAttachment.apply)
 *                  적용 직후 checkChainTriggers 호출 시
 *                  → triggerContext 스태틱 필드로 트리거 종류 캡처
 *             효과: 전기 피해 + 가장 최근 연계 트리거 반복
 *                  (물리 이상이면 동일 이상 재부여, 아츠 부착이면 동일 속성 재부착)
 *
 * [궁극기]   전기 증폭 + 열기 증폭 (AntalAmplificationBuff 부여)
 *             + 즉흥적인 천재성: 버프 지속 중 메인의 기본공격/배틀스킬 전기·열기 적중 시 소량 회복
 *             (팀 연계기 발동 중에는 미발동 — Hero.chainActivationContext 플래그 사용)
 */
public class Antal extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 전기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 0.8f;

    /** 연계기 전기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.7f;

    /** 아츠유닛 최대 충전. */
    private static final int MAX_CHARGES = 3;

    /** 충전 1개당 물리 피해 감소 지속 턴. TODO: 수치 확정 */
    private static final int PHYS_REDUCTION_TURNS_PER_CHARGE = 2;

    // ─────────────────────────────────────────────
    // 충전 (아츠유닛 배틀스킬 충전 시스템 — 플레이스홀더)
    // ─────────────────────────────────────────────

    private int charges = 0;

    // ─────────────────────────────────────────────
    // 연계기 트리거 저장
    // chainCondition 시점에 triggerContext를 읽어 저장.
    // activateChain 시점에 이 값을 사용해 효과 반복.
    // ─────────────────────────────────────────────

    /** 연계기를 트리거한 물리 이상 종류. null이면 물리 이상이 아님. */
    private DefenselessStack.PhysicalAbnormality savedPhysicalType = null;

    /** 연계기를 트리거한 아츠 부착 종류. null이면 아츠 부착이 아님. */
    private ArtsAttachment.ArtsType savedArtsType = null;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "안탈"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.SUPPORTER; }
    @Override public WeaponType weaponType()     { return WeaponType.ARTS_UNIT; }
    @Override public Attribute attribute()       { return Attribute.ELECTRIC; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 전기장 형성
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "전기장 형성"; }
            @Override public String description() {
                return "전기 피해(×" + SKILL_MULT + ") + 전기 취약 + 열기 취약 동시 부여.\n" +
                       "아츠유닛 충전 +1 (최대 " + MAX_CHARGES + "). 충전 효과: 물리 피해 감소.\n" +
                       "TODO: 물리 피해 감소 버프 구현 후 연동";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 기존 충전 소모 → 물리 피해 감소 (TODO: 실제 감소 버프 구현)
                if (charges > 0) {
                    int reductionTurns = charges * PHYS_REDUCTION_TURNS_PER_CHARGE;
                    charges = 0;
                    // TODO: Buff.affect(hero, AntalPhysReductionBuff.class, reductionTurns);
                }

                // 전기 피해
                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.ELECTRIC);

                if (!target.isAlive()) {
                    // 충전 추가 (대상 사망 시에도)
                    if (charges < MAX_CHARGES) charges++;
                    return;
                }

                // 전기 취약 + 열기 취약 부여
                ElectricVulnerable.apply(target);
                HeatVulnerable.apply(target);

                // 아츠유닛 충전 +1
                if (charges < MAX_CHARGES) charges++;
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 적응형 반응
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 2; } // TODO: 수치 확정

    @Override public String chainName()        { return "적응형 반응"; }
    @Override public String chainDescription() {
        return "조건: 대상에게 물리 이상 or 아츠 부착 부여 시\n" +
               "효과: 전기 피해(×" + CHAIN_MULT + ") + 트리거 반복\n" +
               "  (물리 이상이면 동일 이상 재부여, 아츠 부착이면 동일 속성 재부착)";
    }

    /**
     * 연계기 조건:
     * DefenselessStack.triggerContext 또는 ArtsAttachment.triggerContext가 non-null.
     * 이 메서드에서 해당 값을 인스턴스 필드에 저장해 activateChain에서 사용.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;

        if (DefenselessStack.triggerContext != null) {
            // 물리 이상 트리거 저장
            savedPhysicalType = DefenselessStack.triggerContext;
            savedArtsType = null;
            return true;
        }
        if (ArtsAttachment.triggerContext != null) {
            // 아츠 부착 트리거 저장
            savedArtsType = ArtsAttachment.triggerContext;
            savedPhysicalType = null;
            return true;
        }
        return false;
    }

    /** 연계기 효과: 전기 피해 + 저장된 트리거 반복 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 전기 피해
        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.ELECTRIC);

        if (!target.isAlive()) {
            savedPhysicalType = null;
            savedArtsType = null;
            return;
        }

        // 트리거 반복
        if (savedPhysicalType != null) {
            DefenselessStack.apply(target, savedPhysicalType, hero);
            savedPhysicalType = null;
        } else if (savedArtsType != null) {
            ArtsAttachment.apply(target, savedArtsType, hero);
            savedArtsType = null;
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 즉흥적인 천재성
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "즉흥적인 천재성"; }
            @Override public String description() {
                return AntalAmplificationBuff.DURATION + "턴 지속: 전기·열기 피해 × " + AntalAmplificationBuff.AMP_MULT + " 증폭.\n" +
                       "지속 중 기본공격/배틀스킬 전기·열기 적중 시 HP +" + AntalAmplificationBuff.GENIUS_HEAL + " 회복.\n" +
                       "(팀 연계기 발동 중 미발동)";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // 기존 버프 갱신
                AntalAmplificationBuff existing = hero.buff(AntalAmplificationBuff.class);
                if (existing != null) existing.detach();
                Buff.affect(hero, AntalAmplificationBuff.class);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기 (charges + 저장 트리거)
    // ─────────────────────────────────────────────

    private static final String CHARGES           = "charges";
    private static final String SAVED_PHYS_TYPE   = "savedPhysicalType";
    private static final String SAVED_ARTS_TYPE   = "savedArtsType";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CHARGES, charges);
        bundle.put(SAVED_PHYS_TYPE, savedPhysicalType != null ? savedPhysicalType.name() : "");
        bundle.put(SAVED_ARTS_TYPE, savedArtsType != null ? savedArtsType.name() : "");
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        charges = bundle.getInt(CHARGES);

        String pt = bundle.getString(SAVED_PHYS_TYPE);
        savedPhysicalType = (pt != null && !pt.isEmpty())
                ? DefenselessStack.PhysicalAbnormality.valueOf(pt) : null;

        String at = bundle.getString(SAVED_ARTS_TYPE);
        savedArtsType = (at != null && !at.isEmpty())
                ? ArtsAttachment.ArtsType.valueOf(at) : null;
    }
}
