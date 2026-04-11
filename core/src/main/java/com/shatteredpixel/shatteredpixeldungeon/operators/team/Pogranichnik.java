/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.IronVow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 포그라니치니크 (Pogranichnik)
 *
 * 직군: 뱅가드
 * 무기: 한손검
 * 속성: 물리
 *
 * [배틀스킬] 갑옷 관통 일격
 *   - 물리 피해(×SKILL_MULT) + 갑옷파괴(ARMOR_BREAK)
 *   - 소모된 방어불능 스택 비례 궁극기 충전 (스택 1당 CHARGE_PER_STACK)
 *
 * [연계기]   서약의 추격
 *   - 조건: 강타(HEAVY_ATTACK) or 갑옷파괴(ARMOR_BREAK)로 방어불능 스택 소모 시
 *   - 효과: 소모 스택 비례 물리 피해(소모량 × CHAIN_MULT_PER_STACK) + 궁극기 충전
 *
 * [궁극기]   철의 서약 선언
 *   - 물리 피해(×ULT_MULT) + 적에게 철의 서약(IronVow) 3스택 부여
 *
 * [특수]     철의 서약(IronVow)
 *   - 적이 물리 이상 or 연계기를 받을 때마다 1스택 소모 → 물리 피해
 *   - 마지막 스택 소모 시 대량 물리 피해
 *
 * TODO: 모든 피해 수치 확정
 */
public class Pogranichnik extends TeamOperator {

    // ─────────────────────────────────────────────
    // 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 물리 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT              = 1.2f;

    /** 연계기 소모 스택 1개당 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT_PER_STACK    = 0.8f;

    /** 배틀스킬/연계기: 소모 스택 1개당 궁극기 충전량. TODO: 수치 확정 */
    private static final int   CHARGE_PER_STACK        = 10;

    /** 궁극기 기본 물리 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT                = 2.0f;

    /** 궁극기로 부여하는 철의 서약 스택 수 */
    private static final int   IRON_VOW_STACKS         = 3;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "포그라니치니크"; }

    @Override
    public String chainFaceAsset() { return Assets.Operators.POGRANICHNIK_FACE; } // TODO: 에셋 확정 후 경로 지정

    @Override
    public OperatorClass operatorClass() { return OperatorClass.VANGUARD; }

    @Override
    public WeaponType weaponType() { return WeaponType.ONE_HANDED_SWORD; }

    @Override
    public Attribute attribute() { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 갑옷 관통 일격
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 3; // 뱅가드 — 짧은 쿨타임. TODO: 수치 확정
            }

            @Override
            public String name() { return "갑옷 관통 일격"; }

            @Override
            public String description() {
                return "물리 피해(" + SKILL_MULT + "×) + 갑옷파괴(ARMOR_BREAK).\n" +
                       "소모 스택 1개당 궁극기 " + CHARGE_PER_STACK + " 충전.\n" +
                       "TODO: 피해·충전 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 물리 피해
                int damage = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                if (target.isAlive()) {
                    // 갑옷파괴 적용 — 내부적으로 lastConsumedStacks 갱신됨
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.ARMOR_BREAK, hero);

                    // 소모 스택 비례 궁극기 충전
                    int consumed = DefenselessStack.lastConsumedStacks;
                    if (consumed > 0 && hero.activeUltimate != null) {
                        hero.activeUltimate.addCharge(consumed * CHARGE_PER_STACK);
                    }
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 서약의 추격
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 2; // 뱅가드 — 짧은 쿨타임. TODO: 수치 확정
    }

    @Override
    public String chainName() { return "서약의 추격"; }

    @Override
    public String chainDescription() {
        return "조건: 강타 or 갑옷파괴로 방어불능 스택 소모 시\n" +
               "효과: 소모 스택 비례 물리 피해 + 궁극기 충전";
    }

    /**
     * 연계기 조건: 강타(HEAVY_ATTACK) or 갑옷파괴(ARMOR_BREAK)로 방어불능 스택이
     * 소모된 직후. DefenselessStack.lastConsumedStacks > 0으로 판단.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return DefenselessStack.lastConsumedStacks > 0;
    }

    /**
     * 연계기 효과: 소모 스택 비례 물리 피해 + 궁극기 충전.
     * lastConsumedStacks를 읽은 뒤 리셋.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int consumed = DefenselessStack.lastConsumedStacks;
        DefenselessStack.lastConsumedStacks = 0; // 연계기 발동 시 소비

        // 소모 스택 비례 물리 피해
        int damage = Math.round(hero.damageRoll() * CHAIN_MULT_PER_STACK * Math.max(consumed, 1));
        target.damage(damage, hero, DamageType.PHYSICAL);

        // 궁극기 충전
        if (hero.activeUltimate != null) {
            hero.activeUltimate.addCharge(consumed * CHARGE_PER_STACK);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 철의 서약 선언
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            public String name() { return "철의 서약 선언"; }

            @Override
            public String description() {
                return "물리 피해(" + ULT_MULT + "×) + 철의 서약 " + IRON_VOW_STACKS + "스택 부여.\n" +
                       "철의 서약: 물리 이상/연계기 적중마다 소모 → 물리 피해\n" +
                       "(마지막 스택 소모 시 대량 피해)\n" +
                       "TODO: 피해 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 물리 피해
                int damage = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                // 철의 서약 부여 — Hero(메인 오퍼레이터)에게 부여
                if (target.isAlive()) {
                    IronVow.apply(hero, IRON_VOW_STACKS);
                }
            }
        };
    }
}
