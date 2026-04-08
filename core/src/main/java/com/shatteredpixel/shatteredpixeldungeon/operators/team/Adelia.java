/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsVulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalVulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 아델리아 (Adelia)
 *
 * 직군: 서포터
 * 무기: 아츠유닛
 * 속성: 자연
 *
 * [배틀스킬] 자연 피해(×SKILL_MULT).
 *             부식(ArtsCorrosion) 상태 적 → 소모 → 물리 취약 + 아츠 취약 동시 부여
 * [연계기]   조건: 강력한 일격 적중 시 (hero.finishingBlowContext == true)
 *             효과: 자연 피해 2회 + 부식 부여
 * [궁극기]   주변 적에게 10회 자연 피해 (자기 위치 기준 시야 내)
 *             TODO: 범위를 주변 인접 적으로 좁히기 (정확한 사거리 확정 후)
 * [충전 효과] 일정 턴 소비 → 충전량 비례 생명력 + 허기 회복
 *             TODO: 아츠유닛 충전 시스템 구현 후 연동
 */
public class Adelia extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 자연 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.0f;

    /** 연계기 1타 자연 피해 배율 (2회 반복). TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.7f;

    /** 궁극기 1타 자연 피해 배율. TODO: 수치 확정 */
    private static final float ULT_HIT_MULT = 0.5f;

    /** 궁극기 타격 횟수. */
    private static final int ULT_HIT_COUNT = 10;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "아델리아"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.SUPPORTER; }
    @Override public WeaponType weaponType()     { return WeaponType.ARTS_UNIT; }
    @Override public Attribute attribute()       { return Attribute.NATURE; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 자연 충격
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "자연 충격"; }
            @Override public String description() {
                return "자연 피해(×" + SKILL_MULT + ").\n" +
                       "부식 상태 적 → 소모 → 물리 취약 + 아츠 취약 동시 부여";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.NATURE);

                if (!target.isAlive()) return;

                // 부식 소모 → 물리 취약 + 아츠 취약
                ArtsCorrosion corrosion = target.buff(ArtsCorrosion.class);
                if (corrosion != null) {
                    corrosion.detach();
                    PhysicalVulnerable.apply(target);
                    ArtsVulnerable.apply(target);
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 포식 반응
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "포식 반응"; }
    @Override public String chainDescription() {
        return "조건: 강력한 일격 적중 시\n" +
               "효과: 자연 피해 2회 + 부식 부여";
    }

    /**
     * 연계기 조건: 강력한 일격(피니싱 블로우) 직후 컨텍스트.
     * Hero.onFinishingBlowLanded()에서 finishingBlowContext = true 로 설정됨.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        return hero.finishingBlowContext && target != null && target.isAlive();
    }

    /** 연계기 효과: 자연 피해 2회 + 부식(ArtsCorrosion) 부여 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        for (int i = 0; i < 2; i++) {
            if (!target.isAlive()) break;
            int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
            target.damage(dmg, hero, DamageType.NATURE);
        }

        if (target.isAlive()) {
            ArtsCorrosion.apply(target, 1); // 부식 1스택
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 자연의 폭풍
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "자연의 폭풍"; }
            @Override public String description() {
                return "시야 내 모든 적에게 " + ULT_HIT_COUNT + "회 자연 피해(×" + ULT_HIT_MULT + "/회).";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                for (Char ch : Actor.chars()) {
                    if (ch == hero || ch.alignment == Char.Alignment.ALLY) continue;
                    if (!ch.isAlive()) continue;
                    if (!hero.fieldOfView[ch.pos]) continue;

                    for (int i = 0; i < ULT_HIT_COUNT; i++) {
                        if (!ch.isAlive()) break;
                        int dmg = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                        ch.damage(dmg, hero, DamageType.NATURE);
                    }
                }
            }
        };
    }
}
