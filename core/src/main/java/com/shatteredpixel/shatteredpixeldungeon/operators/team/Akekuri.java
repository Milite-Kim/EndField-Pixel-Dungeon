/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AkekuriUltimateBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HitCounter;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.utils.PathFinder;

/**
 * 아케쿠리 (Akekuri)
 *
 * 직군: 뱅가드
 * 무기: 한손검
 * 속성: 열기
 *
 * [배틀스킬] 2회 열기 피해(×SKILL_HIT_MULT/회) + 열기 부착.
 *             각 타격이 HitCounter 카운트에 포함.
 *             TODO: 승급 시 타수 증가
 *
 * [연계기]   조건: 적 누적 피격 횟수 10회 달성 시
 *             (HitCounter.increment() → thresholdJustHit = true → checkChainTriggers)
 *             효과: 물리 피해(×CHAIN_MULT) + 궁극기 충전
 *
 * [궁극기]   소대 집합:
 *             ① 즉시: 주변 8칸 적 열기 피해(×ULT_MULT) + 팀 오퍼레이터 연계기 쿨타임 즉시 50% 감소
 *             ② 지속(DURATION턴): AkekuriUltimateBuff → 매 ~3.3턴마다 팀 전체 쿨타임 1 추가 감소
 *                (= 약 30% 가속 효과)
 */
public class Akekuri extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 1회당 열기 피해 배율 (2회 반복). TODO: 수치 확정 */
    private static final float SKILL_HIT_MULT = 0.7f;

    /** 연계기 물리 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /** 연계기 궁극기 충전량. TODO: 수치 확정 */
    private static final int CHAIN_ULT_CHARGE = 20;

    /** 궁극기 AOE 열기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 1.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "아케쿠리"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.VANGUARD; }
    @Override public WeaponType weaponType()     { return WeaponType.ONE_HANDED_SWORD; }
    @Override public Attribute attribute()       { return Attribute.FIRE; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 연속 타격
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 3; } // TODO: 수치 확정
            @Override public String name()       { return "연속 타격"; }
            @Override public String description() {
                return "2회 열기 피해(×" + SKILL_HIT_MULT + "/회) + 열기 부착.\n" +
                       "각 타격이 피격 횟수 카운트에 포함.\n" +
                       "TODO: 승급 시 타수 증가";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 2회 열기 피해 (각 타격이 HitCounter 카운트 — Char.damage()의 훅에서 처리)
                for (int i = 0; i < 2; i++) {
                    if (!target.isAlive()) break;
                    int dmg = Math.round(hero.damageRoll() * SKILL_HIT_MULT);
                    target.damage(dmg, hero, DamageType.HEAT);
                }

                // 열기 부착
                if (target.isAlive()) {
                    ArtsAttachment.apply(target, ArtsAttachment.ArtsType.HEAT, hero);
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 십격 폭발
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 2; } // TODO: 수치 확정

    @Override public String chainName()        { return "십격 폭발"; }
    @Override public String chainDescription() {
        return "조건: 적 누적 피격 횟수 " + HitCounter.THRESHOLD + "회 달성 시\n" +
               "효과: 물리 피해(×" + CHAIN_MULT + ") + 궁극기 충전";
    }

    /**
     * 연계기 조건: HitCounter가 임계치에 막 도달한 순간.
     * HitCounter.addHit() → thresholdJustHit = true → checkChainTriggers → 이 메서드 호출.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        HitCounter counter = target.buff(HitCounter.class);
        return counter != null && counter.isThresholdJustHit();
    }

    /**
     * 연계기 효과: 물리 피해 + 궁극기 충전.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.PHYSICAL);

        if (hero.activeUltimate != null) {
            hero.activeUltimate.addCharge(CHAIN_ULT_CHARGE);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 소대 집합
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "소대 집합"; }
            @Override public String description() {
                return "주변 적 열기 피해(×" + ULT_MULT + ") + 팀 연계기 쿨타임 즉시 50% 감소.\n" +
                       AkekuriUltimateBuff.DURATION + "턴 지속: 팀 연계기 쿨타임 약 30% 추가 가속.";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // ① 주변 8칸 적 열기 피해 (AOE)
                for (int offset : PathFinder.NEIGHBOURS8) {
                    int c = hero.pos + offset;
                    if (c < 0 || c >= Dungeon.level.length()) continue;
                    Char ch = Actor.findChar(c);
                    if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) continue;
                    if (!ch.isAlive()) continue;

                    int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                    ch.damage(dmg, hero, DamageType.HEAT);
                }

                // ② 팀 오퍼레이터 연계기 쿨타임 즉시 50% 감소
                for (TeamOperator op : hero.teamOperators) {
                    op.reduceCooldownByHalf();
                }

                // ③ 지속 버프: 쿨타임 30% 추가 가속
                AkekuriUltimateBuff existing = hero.buff(AkekuriUltimateBuff.class);
                if (existing != null) existing.detach();
                Buff.affect(hero, AkekuriUltimateBuff.class);
            }
        };
    }
}
