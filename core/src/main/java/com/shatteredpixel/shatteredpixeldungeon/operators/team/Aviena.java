/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Electrified;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HitCounter;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StrongThunderLance;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ThunderLance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 아비웨나 (Aviena)
 *
 * 직군: 스트라이커
 * 무기: 장병기
 * 속성: 전기
 *
 * [배틀스킬] 썬더랜스 회수:
 *             대상의 ThunderLance + StrongThunderLance를 전량 회수 →
 *             종류·수량 비례 전기 피해 (강력한 랜스가 더 높은 배율)
 *
 * [연계기]   조건:
 *             ① 해당 적에게 최초 공격 시 (HitCounter.firstHitContext = true)
 *             ② 전기 부착(ArtsAttachment.ELECTRIC) or 감전(Electrified) 상태 적에게
 *                강력한 일격 적중 시 (finishingBlowContext = true)
 *             효과: 전기 피해 + 썬더랜스 3개 부착
 *
 * [궁극기]   전기 피해 + 강력한 썬더랜스 1개 부착
 */
public class Aviena extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 회수 시 일반 썬더랜스 1개당 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_RECALL_NORMAL_MULT = ThunderLance.RECALL_DMG_MULT;

    /** 배틀스킬 회수 시 강력한 썬더랜스 1개당 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_RECALL_STRONG_MULT = StrongThunderLance.RECALL_DMG_MULT;

    /** 연계기 전기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.9f;

    /** 연계기 적중 시 부착되는 썬더랜스 수. */
    private static final int CHAIN_LANCE_COUNT = 3;

    /** 궁극기 전기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 1.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "아비웨나"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.STRIKER; }
    @Override public WeaponType weaponType()     { return WeaponType.POLEARM; }
    @Override public Attribute attribute()       { return Attribute.ELECTRIC; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 썬더랜스 회수
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "썬더랜스 회수"; }
            @Override public String description() {
                return "부착된 모든 썬더랜스 + 강력한 썬더랜스를 회수 →\n" +
                       "종류·수량 비례 전기 피해 (강력한 썬더랜스 × " + SKILL_RECALL_STRONG_MULT + " / 일반 × " + SKILL_RECALL_NORMAL_MULT + ")";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 부착된 썬더랜스 회수
                int normalCount = 0;
                int strongCount = 0;

                ThunderLance normal = target.buff(ThunderLance.class);
                if (normal != null) {
                    normalCount = normal.count();
                    normal.detach();
                }

                StrongThunderLance strong = target.buff(StrongThunderLance.class);
                if (strong != null) {
                    strongCount = strong.count();
                    strong.detach();
                }

                if (normalCount > 0 || strongCount > 0) {
                    float totalMult = normalCount * SKILL_RECALL_NORMAL_MULT
                            + strongCount * SKILL_RECALL_STRONG_MULT;
                    int dmg = Math.round(hero.damageRoll() * totalMult);
                    target.damage(dmg, hero, DamageType.ELECTRIC);
                }
                // 랜스가 없으면 피해 없음
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 번개 낙뢰
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "번개 낙뢰"; }
    @Override public String chainDescription() {
        return "조건: 최초 타격 시 or 전기 부착/감전 상태 적에게 강력한 일격 시\n" +
               "효과: 전기 피해(×" + CHAIN_MULT + ") + 썬더랜스 " + CHAIN_LANCE_COUNT + "개 부착";
    }

    /**
     * 연계기 조건:
     * ① 해당 적의 HitCounter가 방금 처음 생성(firstHitContext)
     * ② 강력한 일격 직후(finishingBlowContext) + 전기 부착 or 감전 상태
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;

        // ① 최초 타격
        HitCounter counter = target.buff(HitCounter.class);
        if (counter != null && counter.isFirstHitJust()) return true;

        // ② 강력한 일격 + 전기 부착/감전
        if (hero.finishingBlowContext) {
            ArtsAttachment attachment = target.buff(ArtsAttachment.class);
            if (attachment != null && attachment.currentType() == ArtsAttachment.ArtsType.ELECTRIC) return true;
            if (target.buff(Electrified.class) != null) return true;
        }

        return false;
    }

    /** 연계기 효과: 전기 피해 + 썬더랜스 3개 부착 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.ELECTRIC);

        if (target.isAlive()) {
            ThunderLance.attach(target, CHAIN_LANCE_COUNT);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 전기 폭풍
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "전기 폭풍"; }
            @Override public String description() {
                return "전기 피해(×" + ULT_MULT + ") + 강력한 썬더랜스 1개 부착.";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                target.damage(dmg, hero, DamageType.ELECTRIC);

                if (target.isAlive()) {
                    StrongThunderLance.attach(target, 1);
                }
            }
        };
    }
}
