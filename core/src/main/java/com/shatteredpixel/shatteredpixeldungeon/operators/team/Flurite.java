/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 플루라이트 (Flurite)
 *
 * 직군: 캐스터
 * 무기: 권총
 * 속성: 자연
 *
 * [배틀스킬] 자연 피해(×SKILL_MULT) + 자연 부착 + 감속(Cripple)
 * [연계기]   조건: 적 냉기 or 자연 부착 2스택 이상 시
 *             효과: 자연 피해(×CHAIN_MULT) + 재부착 (동일 속성 한 번 더 → 스택 +1 + 아츠 폭발 자동 발동)
 * [궁극기]   4회 자연 피해(×ULT_HIT_MULT/회). 감속 시 피해 증가(×ULT_SLOW_BONUS).
 *             적 냉기/자연 부착 2스택 이상 보유 시 재부착 (동일 속성 apply → 폭발)
 */
public class Flurite extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 자연 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.0f;

    /** 연계기 자연 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.8f;

    /** 궁극기 1타 자연 피해 배율. TODO: 수치 확정 */
    private static final float ULT_HIT_MULT = 0.7f;

    /** 궁극기 감속 적 추가 피해 배율 (ULT_HIT_MULT에 곱). TODO: 수치 확정 */
    private static final float ULT_SLOW_BONUS = 1.5f;

    /** 재부착 발동을 위한 최소 스택 수. */
    private static final int REATTACH_THRESHOLD = 2;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "플루라이트"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.CASTER; }
    @Override public WeaponType weaponType()     { return WeaponType.HANDGUN; }
    @Override public Attribute attribute()       { return Attribute.NATURE; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 자연 탄환
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 4; } // TODO: 수치 확정
            @Override public String name()       { return "자연 탄환"; }
            @Override public String description() {
                return "자연 피해(×" + SKILL_MULT + ") + 자연 부착 + 감속.";
            }

            @Override public int range() { return 5; } // 권총

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.NATURE);

                if (!target.isAlive()) return;

                ArtsAttachment.apply(target, ArtsAttachment.ArtsType.NATURE, hero);
                Buff.affect(target, Cripple.class, Cripple.DURATION);
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 재부착 사격
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "재부착 사격"; }
    @Override public String chainDescription() {
        return "조건: 적 냉기 or 자연 부착 " + REATTACH_THRESHOLD + "스택 이상\n" +
               "효과: 자연 피해 + 재부착 (동일 속성 +1스택 → 아츠 폭발 자동 발동)";
    }

    /**
     * 연계기 조건: 적이 냉기 or 자연 부착을 2스택 이상 보유.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        if (arts == null) return false;
        return (arts.currentType() == ArtsAttachment.ArtsType.CRYO
                || arts.currentType() == ArtsAttachment.ArtsType.NATURE)
                && arts.stacks() >= REATTACH_THRESHOLD;
    }

    /**
     * 연계기 효과: 자연 피해 + 재부착.
     * 재부착 = 현재 보유 속성과 동일한 속성으로 apply() → 스택 +1 + 아츠 폭발 자동 발동.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.NATURE);

        if (!target.isAlive()) return;

        // 재부착: 현재 보유 속성 그대로 한 번 더 apply → 동일 속성 재부착 → 아츠 폭발
        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        if (arts != null) {
            ArtsAttachment.apply(target, arts.currentType(), hero);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 자연 파동
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "자연 파동"; }
            @Override public String description() {
                return "4회 자연 피해(×" + ULT_HIT_MULT + ", 감속 시 ×" + ULT_SLOW_BONUS + " 추가).\n" +
                       "냉기/자연 부착 " + REATTACH_THRESHOLD + "스택 이상 시 재부착.";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                boolean isSlowed = target.buff(Cripple.class) != null;

                for (int i = 0; i < 4; i++) {
                    if (!target.isAlive()) break;
                    float mult = isSlowed ? ULT_HIT_MULT * ULT_SLOW_BONUS : ULT_HIT_MULT;
                    int dmg = Math.round(hero.damageRoll() * mult);
                    target.damage(dmg, hero, DamageType.NATURE);
                }

                // 냉기/자연 부착 2스택 이상 → 재부착
                if (target.isAlive()) {
                    ArtsAttachment arts = target.buff(ArtsAttachment.class);
                    if (arts != null
                            && (arts.currentType() == ArtsAttachment.ArtsType.CRYO
                                || arts.currentType() == ArtsAttachment.ArtsType.NATURE)
                            && arts.stacks() >= REATTACH_THRESHOLD) {
                        ArtsAttachment.apply(target, arts.currentType(), hero);
                    }
                }
            }
        };
    }
}
