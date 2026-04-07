/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArmorBreaked;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FollowUp;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 여풍 (Yeofung)
 *
 * 직군: 가드
 * 무기: 장병기
 * 속성: 물리
 *
 * [배틀스킬] 도깨비 창
 *   - 물리 피해(×SKILL_MULT) + 넘어뜨리기(KNOCKDOWN)
 *   - 방어불능 스택 없는 적 적중 시 → 물리 취약(Vulnerable) 추가 부여
 *
 * [연계기]   연타 선구
 *   - 조건: 물리 취약(Vulnerable) or 갑옷파괴(ArmorBreaked) 상태 적에게 메인의 강력한 일격 적중 시
 *   - 효과: 물리 피해(×CHAIN_MULT) + 메인에게 연타(FollowUp) 버프 부여
 *
 * [궁극기]   쌍익창
 *   - 2회: 물리 피해(×ULT_HIT_MULT) + 넘어뜨리기
 *   - 연타(FollowUp) 보유 시 소모 → 추가 대량 물리 피해(×ULT_FOLLOWUP_MULT)
 *
 * [특수]     연타 — 다음 배틀스킬 or 궁극기 피해 1회 추가
 *
 * TODO: 모든 피해 수치 확정
 */
public class Yeofung extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해 배율 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 물리 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT          = 1.2f;

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT          = 1.0f;

    /** 궁극기 1회당 물리 피해 배율 (2회). TODO: 수치 확정 */
    private static final float ULT_HIT_MULT        = 1.0f;

    /** 궁극기 연타 소모 시 추가 대량 피해 배율. TODO: 수치 확정 */
    private static final float ULT_FOLLOWUP_MULT   = 2.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "여풍"; }

    @Override
    public OperatorClass operatorClass() { return OperatorClass.GUARD; }

    @Override
    public WeaponType weaponType() { return WeaponType.POLEARM; }

    @Override
    public Attribute attribute() { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 도깨비 창
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 4; // TODO: 수치 확정
            }

            @Override
            public String name() { return "도깨비 창"; }

            @Override
            public String description() {
                return "물리 피해(" + SKILL_MULT + "×) + 넘어뜨리기.\n" +
                       "방어불능 스택 없는 적 → 물리 취약 추가 부여.\n" +
                       "TODO: 피해 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 방어불능 스택 보유 여부 확인 (피해 전 체크)
                boolean hasNoStack = target.buff(DefenselessStack.class) == null;

                // 물리 피해
                int damage = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                if (target.isAlive()) {
                    // 넘어뜨리기
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.KNOCKDOWN, hero);

                    // 방어불능 스택 없던 적에게 물리 취약 추가 부여
                    if (hasNoStack) {
                        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(
                                target, Vulnerable.class, Vulnerable.DURATION);
                    }
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 연타 선구
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    @Override
    public String chainName() { return "연타 선구"; }

    @Override
    public String chainDescription() {
        return "조건: 물리 취약 or 갑옷파괴 상태 적에게 메인의 강력한 일격 적중 시\n" +
               "효과: 물리 피해 + 메인에게 연타(FollowUp) 버프 부여\n" +
               "연타: 다음 배틀스킬/궁극기 피해 1회 추가";
    }

    /**
     * 연계기 조건: 대상에게 물리 취약(Vulnerable) or 갑옷파괴(ArmorBreaked) 버프 존재.
     * Hero.onFinishingBlowLanded() → checkChainTriggers() 경로로 평가됨.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(Vulnerable.class) != null
                || target.buff(ArmorBreaked.class) != null;
    }

    /** 연계기 효과: 물리 피해 + 메인에게 연타 버프 부여 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 물리 피해
        int damage = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(damage, hero, DamageType.PHYSICAL);

        // 메인 오퍼레이터에게 연타 버프 부여
        FollowUp.apply(hero);
    }

    // ─────────────────────────────────────────────
    // 궁극기: 쌍익창
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            public String name() { return "쌍익창"; }

            @Override
            public String description() {
                return "2회 물리 피해(" + ULT_HIT_MULT + "×/회) + 넘어뜨리기.\n" +
                       "연타(FollowUp) 보유 시 소모 → 추가 대량 물리 피해(" + ULT_FOLLOWUP_MULT + "×).\n" +
                       "TODO: 피해 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 2회 물리 피해 + 넘어뜨리기
                for (int i = 0; i < 2; i++) {
                    if (!target.isAlive()) break;
                    int damage = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                    target.damage(damage, hero, DamageType.PHYSICAL);
                    if (target.isAlive()) {
                        DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.KNOCKDOWN, hero);
                    }
                }

                // 연타 소모 → 추가 대량 피해
                if (target.isAlive()) {
                    FollowUp fu = hero.buff(FollowUp.class);
                    if (fu != null && fu.consume()) {
                        int bonusDamage = Math.round(hero.damageRoll() * ULT_FOLLOWUP_MULT);
                        target.damage(bonusDamage, hero, DamageType.PHYSICAL);
                    }
                }
            }
        };
    }
}
