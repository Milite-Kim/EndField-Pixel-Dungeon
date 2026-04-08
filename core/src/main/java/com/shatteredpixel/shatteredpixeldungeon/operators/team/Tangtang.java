/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsVulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Whirlpool;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;

/**
 * 탕탕 (Tangtang)
 *
 * 직군: 캐스터
 * 무기: 권총
 * 속성: 냉기
 *
 * [배틀스킬] 냉기 피해(×SKILL_MULT) + 냉기 부착.
 *             와류 보유 시: 전량 소모 → 소모량×SKILL_WHIRL_MULT 추가 냉기 피해 + 아츠 취약
 * [연계기]   조건: 적 냉기 부착 보유 시 (냉기 부착 부여 or 아츠 폭발 후 checkChainTriggers 호출 시 감지)
 *             효과: 냉기 피해(×CHAIN_MULT) + 와류 +1 (Hero에게, 최대 2)
 * [궁극기]   냉기 피해(×ULT_HIT_MULT) 전체 범위 + 와류 +1.
 *             지속 중 배틀스킬 시전 시 즉시 종료.
 *             TODO: 지속 냉기 피해 지대 + 배틀스킬 즉시 종료 — Blob 시스템 연동 후 구현
 */
public class Tangtang extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 냉기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 1.0f;

    /** 배틀스킬 와류 소모 시 스택당 추가 냉기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_WHIRL_MULT = 0.5f;

    /** 연계기 냉기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 0.8f;

    /** 궁극기 냉기 피해 배율 (범위 타격). TODO: 수치 확정 */
    private static final float ULT_HIT_MULT = 1.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "탕탕"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.CASTER; }
    @Override public WeaponType weaponType()     { return WeaponType.HANDGUN; }
    @Override public Attribute attribute()       { return Attribute.COLD; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 와류 사격
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown() { return 4; } // TODO: 수치 확정
            @Override public String name()        { return "와류 사격"; }
            @Override public String description() {
                return "냉기 피해(×" + SKILL_MULT + ") + 냉기 부착.\n" +
                       "와류 보유 시: 전량 소모 → 스택당 추가 냉기 피해(×" + SKILL_WHIRL_MULT + "/스택) + 아츠 취약";
            }

            @Override public int range() { return 5; } // 권총: 넓은 사거리

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                // 기본 냉기 피해 + 냉기 부착
                int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(dmg, hero, DamageType.COLD);

                if (!target.isAlive()) return;

                ArtsAttachment.apply(target, ArtsAttachment.ArtsType.CRYO, hero);

                // 와류 소모 → 추가 냉기 피해 + 아츠 취약
                Whirlpool whirlpool = hero.buff(Whirlpool.class);
                if (whirlpool != null) {
                    int consumed = whirlpool.consumeAll();
                    if (consumed > 0 && target.isAlive()) {
                        int bonusDmg = Math.round(hero.damageRoll() * SKILL_WHIRL_MULT * consumed);
                        target.damage(bonusDmg, hero, DamageType.COLD);

                        if (target.isAlive()) {
                            ArtsVulnerable.apply(target);
                        }
                    }
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 냉기 와류탄
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 3; } // TODO: 수치 확정

    @Override public String chainName()        { return "냉기 와류탄"; }
    @Override public String chainDescription() {
        return "조건: 적 냉기 부착 보유 시\n" +
               "효과: 냉기 피해 + 와류 +1 (Hero, 최대 " + Whirlpool.MAX_STACKS + ")";
    }

    /**
     * 연계기 조건: 적이 냉기(CRYO) 부착을 보유 중.
     * 냉기 부착 부여 or 아츠 폭발 후 checkChainTriggers 호출 시 자동 감지.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        return arts != null && arts.currentType() == ArtsAttachment.ArtsType.CRYO;
    }

    /** 연계기 효과: 냉기 피해 + Hero에게 와류 +1 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.COLD);

        // Hero에게 와류 +1
        Whirlpool whirlpool = hero.buff(Whirlpool.class);
        if (whirlpool == null) {
            whirlpool = Buff.affect(hero, Whirlpool.class);
        }
        whirlpool.addStack();
    }

    // ─────────────────────────────────────────────
    // 궁극기: 냉기 폭풍 (자기 위치 기준 범위형)
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "냉기 폭풍"; }
            @Override public String description() {
                return "시야 내 모든 적에게 냉기 피해(×" + ULT_HIT_MULT + ") + 와류 +1.\n" +
                       "TODO: 냉기 피해 지대 지속 + 배틀스킬 시전 시 즉시 종료 — Blob 연동 후 구현";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // 시야 내 모든 적에게 냉기 피해
                for (Char ch : Actor.chars()) {
                    if (ch == hero || ch.alignment == Char.Alignment.ALLY) continue;
                    if (!ch.isAlive()) continue;
                    if (!hero.fieldOfView[ch.pos]) continue;

                    int dmg = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                    ch.damage(dmg, hero, DamageType.COLD);
                }

                // 와류 +1
                Whirlpool whirlpool = hero.buff(Whirlpool.class);
                if (whirlpool == null) {
                    whirlpool = Buff.affect(hero, Whirlpool.class);
                }
                whirlpool.addStack();

                // TODO: 냉기 피해 지대 생성 (Blob 시스템 연동 후 구현)
            }
        };
    }
}
