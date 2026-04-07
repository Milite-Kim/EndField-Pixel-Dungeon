/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.OriginiumCrystal;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 관리자 (Endministrator)
 *
 * 직군: 가드
 * 무기: 한손검
 * 속성: 물리
 *
 * [배틀스킬] 강타 — HEAVY_ATTACK 방어불능 스택 적용 (스택 비례 물리 피해)
 * [연계기]   오리지늄 아츠 투척
 *   - 조건: 아군 연계기 적중 시 (자신 제외)
 *   - 효과: 물리 피해 + 오리지늄 아츠 결정 1스택 부여
 * [궁극기]   오리지늄 폭풍
 *   - 대량 물리 피해
 *   - 오리지늄 아츠 결정 보유 시 전량 소모 → 스택 비례 추가 물리 피해
 * [특수]     오리지늄 아츠 결정
 *   - 적이 물리 이상(DefenselessStack)을 받을 때 1스택 소모 → 물리 피해
 *
 * ※ 가드임에도 방어불능 스택 직접 축적 불가.
 *   팀 운용 시 물리 이상 전반에 반응.
 * ※ 기본 해금 오퍼레이터 (첫 게임 시작 시 보유)
 */
public class Endministrator extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해 배율 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /** 궁극기 기본 물리 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 2.5f;

    /** 궁극기 오리지늄 아츠 결정 스택당 추가 피해 배율. TODO: 수치 확정 */
    private static final float ULT_CRYSTAL_MULT = 0.8f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "관리자"; }

    @Override
    public OperatorClass operatorClass() { return OperatorClass.GUARD; }

    @Override
    public WeaponType weaponType() { return WeaponType.ONE_HANDED_SWORD; }

    @Override
    public Attribute attribute() { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 강타
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 4; // TODO: 수치 확정
            }

            @Override
            public String name() { return "강타"; }

            @Override
            public String description() {
                return "강타(HEAVY_ATTACK) — 스택 전량 소모 + 스택 비례 대량 물리 피해.\n" +
                       "TODO: 피해 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;
                DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.HEAVY_ATTACK, hero);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 오리지늄 아츠 투척
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    @Override
    public String chainName() { return "오리지늄 아츠 투척"; }

    @Override
    public String chainDescription() {
        return "조건: 아군 연계기 적중 시\n" +
               "효과: 물리 피해 + 오리지늄 아츠 결정 1스택 부여\n" +
               "결정: 적이 물리 이상 받을 때 소모 → 물리 피해";
    }

    /**
     * 연계기 조건: 아군 연계기 적중 시 (자기 자신 제외).
     * Hero.activateFrontChain()에서 lastChainActivator가 설정된 상태로 호출됨.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        return hero.lastChainActivator != null
                && !(hero.lastChainActivator instanceof Endministrator)
                && target != null && target.isAlive();
    }

    /** 연계기 효과: 물리 피해 + 오리지늄 아츠 결정 1스택 부여 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int damage = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(damage, hero, DamageType.PHYSICAL);

        if (target.isAlive()) {
            OriginiumCrystal.apply(target, 1);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 오리지늄 폭풍
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            public String name() { return "오리지늄 폭풍"; }

            @Override
            public String description() {
                return "대량 물리 피해(" + ULT_MULT + "×).\n" +
                       "오리지늄 아츠 결정 보유 시 전량 소모 → 스택 비례 추가 물리 피해.\n" +
                       "TODO: 피해 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 기본 물리 피해
                int damage = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                // 오리지늄 아츠 결정 소모 → 추가 물리 피해
                if (target.isAlive()) {
                    OriginiumCrystal crystal = target.buff(OriginiumCrystal.class);
                    if (crystal != null) {
                        int consumed = crystal.consumeAll();
                        if (consumed > 0) {
                            int bonusDamage = Math.round(hero.damageRoll() * ULT_CRYSTAL_MULT * consumed);
                            target.damage(bonusDamage, hero, DamageType.PHYSICAL);
                        }
                    }
                }
            }
        };
    }
}
