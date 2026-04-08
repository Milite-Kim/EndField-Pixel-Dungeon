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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.KachirParry;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 카치르 (Kachir)
 *
 * 직군: 디펜더
 * 무기: 양손검
 * 속성: 물리
 *
 * [배틀스킬] 다음 물리 공격 방어 (물리 100% 차단) → 성공 시 카운터: 물리 피해 + 방어불능 1스택
 *             TODO: 아츠 70% 감소는 DamageType-aware 훅 구현 후 추가
 * [연계기]   조건: 적 차지 시작 시 (Charging 버프 감지)
 *             효과: 물리 피해(×CHAIN_MULT) + 쉴드(Barrier) 부여
 * [궁극기]   3회 대량 물리 피해(×ULT_HIT_MULT/회) + 넘어뜨리기(KNOCKDOWN) + 허약(Vulnerable) 부여
 *
 * ※ 차지 패턴 중단 불가 / 쉴드로 맞딜 or 회피 선택
 */
public class Kachir extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 연계기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.2f;

    /**
     * 연계기 쉴드량 (Hero HT 대비 비율).
     * 카치르는 '높은 수치 쉴드'가 컨셉이므로 넉넉한 값으로 설정.
     * TODO: 수치 확정
     */
    private static final float SHIELD_RATIO = 0.4f;

    /** 궁극기 히트 수 */
    private static final int ULT_HIT_COUNT = 3;

    /** 궁극기 히트 1회당 피해 배율. TODO: 수치 확정 */
    private static final float ULT_HIT_MULT = 1.2f;

    /** 허약 지속 시간. TODO: 수치 확정 */
    private static final float VULNERABLE_DURATION = Vulnerable.DURATION;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "카치르"; }

    @Override
    public OperatorClass operatorClass() { return OperatorClass.DEFENDER; }

    @Override
    public WeaponType weaponType() { return WeaponType.TWO_HANDED_SWORD; }

    @Override
    public Attribute attribute() { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 패링 준비
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 4; // TODO: 수치 확정
            }

            @Override
            public String name() { return "철벽 패링"; } // TODO: 정식 스킬명 확정

            @Override
            public String description() {
                return "다음 물리 공격을 완전 차단.\n" +
                       "방어 성공 시 카운터: 물리 피해(×" + KachirParry.COUNTER_MULT + ") + 방어불능 1스택.\n" +
                       "TODO: 아츠 70% 감소 추가 예정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // 대상 없음 — 자기 자신에게 패링 버프 부여
                Buff.affect(hero, KachirParry.class);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 물리 피해 + 쉴드
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    @Override
    public String chainName() { return "방어 반격"; } // TODO: 정식 스킬명 확정

    @Override
    public String chainDescription() {
        return "조건: 적 차지 시작 시\n" +
               "물리 피해(×" + CHAIN_MULT + ") + Hero에게 쉴드(최대HP×" + SHIELD_RATIO + ") 부여";
    }

    /**
     * 연계기 조건: 적이 차지 동작을 시작했을 때 (Charging 버프 보유).
     * 차지 시작 시 Charging.startCharge(enemy) 로 트리거됨.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(Charging.class) != null;
    }

    /**
     * 연계기 효과: 물리 피해 + 쉴드 부여.
     * 차지를 중단시키지는 않음 (중단 불가 컨셉).
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 물리 피해
        int damage = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(damage, hero, DamageType.PHYSICAL);

        // Hero에게 쉴드 부여 (Barrier)
        int shieldAmt = Math.round(hero.HT * SHIELD_RATIO);
        Buff.affect(hero, Barrier.class).incShield(shieldAmt);
    }

    // ─────────────────────────────────────────────
    // 궁극기: 3회 대량 물리 피해 + 넘어뜨리기 + 허약
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            public String name() { return "파쇄 심판"; } // TODO: 정식 스킬명 확정

            @Override
            public String description() {
                return ULT_HIT_COUNT + "회 대량 물리 피해(×" + ULT_HIT_MULT + "/회) + 넘어뜨리기 + 허약 부여.\n" +
                       "TODO: 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 3회 대량 물리 피해
                for (int i = 0; i < ULT_HIT_COUNT; i++) {
                    if (!target.isAlive()) break;
                    int damage = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                    target.damage(damage, hero, DamageType.PHYSICAL);
                }

                // 넘어뜨리기
                if (target.isAlive()) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.KNOCKDOWN, hero);
                }

                // 허약 부여
                if (target.isAlive()) {
                    Buff.affect(target, Vulnerable.class, VULNERABLE_DURATION);
                }
            }
        };
    }
}
