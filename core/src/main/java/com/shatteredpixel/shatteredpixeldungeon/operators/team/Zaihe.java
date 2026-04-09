/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SupportCrystal;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

/**
 * 자이히 (Zaihe)
 *
 * 직군: 서포터
 * 무기: 아츠유닛
 * 속성: 냉기
 *
 * [배틀스킬] 지원 결정체 소환 (최대 3회 강력한 일격 시 추가 냉기 피해 + 아츠 증폭. 갱신만, 중첩 불가)
 *             TODO: 아츠유닛 충전 +1 시스템 구현 후 연동
 * [연계기]   조건: 아군 배틀스킬 적중 시 (lastChainActivator 기반이 아닌 배틀스킬 훅 필요)
 *             → 현재: 적 냉기 or 자연 부착 보유 시로 근사 구현
 *             효과: 냉기 피해 + 냉기 부착
 * [궁극기]   냉기 증폭 + 자연 증폭 부여 (자기 강화형)
 *             결정체 소환 중이라면 소모 → 냉기 피해 + 소량 Hero 회복
 *             TODO: 냉기/자연 증폭 버프 시스템 구현 후 연동
 * [충전 효과] 충전 전량 소모 → 소모량 비례 배틀스킬 쿨타임 감소
 *             TODO: 아츠유닛 충전 시스템 구현 후 연동
 */
public class Zaihe extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 연계기 냉기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.8f;

    /** 궁극기 결정체 소모 시 냉기 피해 배율. TODO: 수치 확정 */
    private static final float ULT_CRYSTAL_MULT = 1.2f;

    /** 궁극기 결정체 소모 시 Hero 회복량 (최대 HP 비율). TODO: 수치 확정 */
    private static final float ULT_RECOVERY_RATIO = 0.10f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "자이히"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.SUPPORTER; }
    @Override public WeaponType weaponType()     { return WeaponType.ARTS_UNIT; }
    @Override public Attribute attribute()       { return Attribute.COLD; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 지원 결정체 소환
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown() { return 4; } // TODO: 수치 확정
            @Override public String name()        { return "지원 결정체 소환"; }
            @Override public String description() {
                return "지원 결정체 소환 (강력한 일격 시 추가 냉기 피해 × " + SupportCrystal.COLD_MULT + ", 최대 " + SupportCrystal.MAX_HITS + "회).\n" +
                       "재소환 시 남은 횟수 갱신.";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                SupportCrystal.apply(hero);
                gainArtsCharge();
            }
        };
    }

    // ─────────────────────────────────────────────
    // 아츠유닛 충전 활성화: 배틀스킬 쿨타임 감소
    // ─────────────────────────────────────────────

    /**
     * 충전 전량 소모 → 배틀스킬 현재 쿨다운 감소.
     * 감소량: 1충전=1턴, 2충전=3턴, 3충전=5턴 (artsCharges × 2 − 1).
     */
    @Override
    public void activateArtsCharge(Hero hero) {
        if (artsCharges <= 0) return;
        int reduction = artsCharges * 2 - 1;
        artsCharges = 0;
        if (hero.activeBattleSkill != null) {
            hero.activeBattleSkill.reduceCooldownBy(reduction);
        }
    }

    // ─────────────────────────────────────────────
    // 연계기: 냉기 지원 사격
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "냉기 지원 사격"; }
    @Override public String chainDescription() {
        return "조건: 적 냉기 or 자연 부착 보유 시 (TODO: 아군 배틀스킬 적중 시로 교체)\n" +
               "효과: 냉기 피해 + 냉기 부착";
    }

    /**
     * 연계기 조건: 적이 냉기 or 자연 부착을 보유 중일 때.
     * TODO: "아군 배틀스킬 적중 시" 트리거 훅 구현 후 변경
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        return arts != null
                && (arts.currentType() == ArtsAttachment.ArtsType.CRYO
                    || arts.currentType() == ArtsAttachment.ArtsType.NATURE);
    }

    /** 연계기 효과: 냉기 피해 + 냉기 부착. */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.COLD);

        if (target.isAlive()) {
            ArtsAttachment.apply(target, ArtsAttachment.ArtsType.CRYO, hero);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 절대 냉기 (자기 강화형)
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "절대 냉기"; }
            @Override public String description() {
                return "냉기 증폭 + 자연 증폭 부여 (TODO: 증폭 버프 구현 후 연동).\n" +
                       "지원 결정체 소환 중이라면 소모 → 냉기 피해(×" + ULT_CRYSTAL_MULT + ") + Hero 회복";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // TODO: 냉기 증폭 + 자연 증폭 버프 적용

                // 결정체 소모 → 냉기 피해 + Hero 회복
                SupportCrystal crystal = hero.buff(SupportCrystal.class);
                if (crystal != null) {
                    crystal.detach();

                    // 가장 가까운 적에게 냉기 피해
                    Char nearest = null;
                    int minDist = Integer.MAX_VALUE;
                    for (com.shatteredpixel.shatteredpixeldungeon.actors.Actor ch
                            : com.shatteredpixel.shatteredpixeldungeon.actors.Actor.chars()) {
                        if (!(ch instanceof Char)) continue;
                        Char c = (Char) ch;
                        if (c == hero || c.alignment == Char.Alignment.ALLY || !c.isAlive()) continue;
                        int d = com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.distance(hero.pos, c.pos);
                        if (d < minDist) { minDist = d; nearest = c; }
                    }
                    if (nearest != null) {
                        int dmg = Math.round(hero.damageRoll() * ULT_CRYSTAL_MULT);
                        nearest.damage(dmg, hero, DamageType.COLD);
                    }

                    // Hero 소량 회복
                    int recover = Math.round(hero.HT * ULT_RECOVERY_RATIO);
                    hero.HP = Math.min(hero.HP + recover, hero.HT);
                    hero.sprite.showStatus(CharSprite.POSITIVE, "+" + recover);
                }
            }
        };
    }
}
