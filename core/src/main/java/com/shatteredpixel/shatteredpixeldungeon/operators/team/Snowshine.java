/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnowshineParry;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnowshineShield;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 스노우샤인 (Snowshine)
 *
 * 직군: 디펜더
 * 무기: 양손검
 * 속성: 냉기
 *
 * [배틀스킬] 다음 턴까지 물리 방어
 *             → 성공 시: 냉기 피해(×SKILL_COUNTER_MULT) + 냉기 부착
 *             (KachirParry와 유사한 구조, SnowshineParry 버프로 처리)
 *             TODO: 아츠 70% 감소 — DamageType-aware 훅 구현 후 추가
 * [연계기]   조건: 적 차지 시작 시 OR Hero HP 30% 이하
 *             효과: 쉴드(최대HP × SHIELD_RATIO) 부여 → 지속 종료 시 잔여 쉴드 50% HP 회복
 * [궁극기]   대량 냉기 피해(×ULT_MULT) + 냉기 부착 + 적 주변 냉기 피해 지대 생성
 *             TODO: 냉기 지대 — Blob 시스템 연동 후 구현
 *
 * ※ 카치르보다 쉴드량 낮음. 대신 쉴드→HP 전환 보유
 */
public class Snowshine extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 카운터 냉기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_COUNTER_MULT = 0.7f;

    /** 연계기 쉴드량 (Hero HT 대비 비율). 카치르(0.4)보다 낮게. TODO: 수치 확정 */
    private static final float SHIELD_RATIO = 0.25f;

    /** 궁극기 냉기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 2.0f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "스노우샤인"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.DEFENDER; }
    @Override public WeaponType weaponType()     { return WeaponType.TWO_HANDED_SWORD; }
    @Override public Attribute attribute()       { return Attribute.COLD; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 냉기 방어
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown() { return 5; } // TODO: 수치 확정
            @Override public String name()        { return "냉기 방어"; }
            @Override public String description() {
                return "다음 물리 공격 차단 → 성공 시 냉기 피해(×" + SKILL_COUNTER_MULT + ") + 냉기 부착 반격.\n" +
                       "TODO: 아츠 70% 감소";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                Buff.affect(hero, SnowshineParry.class).setCounterMult(SKILL_COUNTER_MULT);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 빙결 방패
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 4; } // TODO: 수치 확정

    @Override public String chainName()        { return "빙결 방패"; }
    @Override public String chainDescription() {
        return "조건: 적 차지 시작 시 OR Hero HP 30% 이하\n" +
               "효과: 쉴드 부여 (최대HP×" + (int)(SHIELD_RATIO*100) + "%) → 종료 시 잔여 쉴드 50% HP 회복";
    }

    /**
     * 연계기 조건: 적이 Charging 상태 OR Hero HP가 최대 HP의 30% 이하.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target != null && target.isAlive() && target.buff(Charging.class) != null) {
            return true;
        }
        return hero.HP <= Math.round(hero.HT * 0.30f);
    }

    /** 연계기 효과: Hero에게 SnowshineShield 부여. */
    @Override
    public void activateChain(Hero hero, Char target) {
        int shieldAmt = Math.round(hero.HT * SHIELD_RATIO);
        SnowshineShield.apply(hero, shieldAmt);
    }

    // ─────────────────────────────────────────────
    // 궁극기: 빙하 강타
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "빙하 강타"; }
            @Override public String description() {
                return "대량 냉기 피해(×" + ULT_MULT + ") + 냉기 부착.\n" +
                       "TODO: 적 주변 냉기 피해 지대 생성 — Blob 시스템 연동 후 구현";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(dmg, hero, DamageType.COLD);

                if (target.isAlive()) {
                    ArtsAttachment.apply(target, ArtsAttachment.ArtsType.CRYO, hero);
                }
                // TODO: 적 주변 냉기 피해 지대(Blob) 생성
            }
        };
    }
}
