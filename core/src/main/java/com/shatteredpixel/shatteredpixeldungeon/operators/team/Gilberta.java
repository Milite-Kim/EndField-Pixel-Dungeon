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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.utils.PathFinder;

/**
 * 질베르타 (Gilberta)
 *
 * 직군: 서포터
 * 무기: 아츠유닛
 * 속성: 자연
 *
 * [배틀스킬] 중력 특이점 소환 (지정 위치 3×3 즉시 자연 피해 + 감속)
 *             TODO: 범위 내 지속 자연 피해 + 감속, 종료 시 폭발(자연 피해 + 자연 부착) — Blob 연동 후 구현
 * [연계기]   조건: 적 아츠 상태이상 보유 시 (ArtsAttachment 혹은 부식/동결/감전/연소 등 아츠 반응 포함)
 *             효과: 자연 피해 + 띄우기(LAUNCH)
 * [궁극기]   중력 혼란 구역 (자기 위치 기준 시야 내 전체)
 *             자연 피해 + 자연 부착 + 감속 + 아츠 취약
 *             + 방어불능 스택 비례 추가 아츠 취약 (스택 높을수록 강화)
 *             TODO: 지속 구역 — Blob 연동 후 구현
 * [충전 효과] 충전 전량 소모 → 소모량 비례 자연 피해 (감속 적 추가 피해)
 *             TODO: 아츠유닛 충전 시스템 구현 후 연동
 */
public class Gilberta extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 단일 타격 자연 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 0.6f;

    /** 연계기 자연 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT = 1.0f;

    /** 궁극기 자연 피해 배율. TODO: 수치 확정 */
    private static final float ULT_MULT = 1.0f;

    /** 궁극기 방어불능 스택당 아츠 취약 추가 지속 시간 (턴). TODO: 수치 확정 */
    private static final float ULT_STACK_BONUS = 1.0f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()               { return "질베르타"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.SUPPORTER; }
    @Override public WeaponType weaponType()     { return WeaponType.ARTS_UNIT; }
    @Override public Attribute attribute()       { return Attribute.NATURE; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 중력 특이점
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown()  { return 5; } // TODO: 수치 확정
            @Override public String name()       { return "중력 특이점"; }
            @Override public String description() {
                return "지정 위치 3×3 자연 피해 + 감속.\n" +
                       "TODO: 지속 자연 피해 + 종료 시 폭발(자연 부착) — Blob 연동 후 구현";
            }

            @Override public int range()             { return 5; }
            @Override public boolean canTargetCell() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                for (int offset : PathFinder.NEIGHBOURS9) {
                    int c = cell + offset;
                    if (c < 0 || c >= Dungeon.level.length()) continue;
                    Char ch = Actor.findChar(c);
                    if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) continue;

                    int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                    ch.damage(dmg, hero, DamageType.NATURE);

                    if (ch.isAlive()) {
                        Buff.affect(ch, Cripple.class, Cripple.DURATION);
                    }
                }
                // TODO: 중력 특이점 Actor/Blob 배치
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 중력 인력
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 4; } // TODO: 수치 확정

    @Override public String chainName()        { return "중력 인력"; }
    @Override public String chainDescription() {
        return "조건: 적 아츠 상태이상 보유 시\n" +
               "효과: 자연 피해 + 띄우기";
    }

    /**
     * 연계기 조건: 적이 아츠 상태이상(부착, 부식, 동결, 감전, 연소) 중 하나라도 보유.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(ArtsAttachment.class) != null
                || target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsCorrosion.class) != null
                || target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frozen.class) != null
                || target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Electrified.class) != null
                || target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combustion.class) != null;
    }

    /** 연계기 효과: 자연 피해 + LAUNCH */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        int dmg = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(dmg, hero, DamageType.NATURE);

        if (target.isAlive()) {
            DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 중력 혼란 구역 (자기 강화형)
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "중력 혼란 구역"; }
            @Override public String description() {
                return "시야 내 모든 적에게 자연 피해 + 자연 부착 + 감속 + 아츠 취약.\n" +
                       "방어불능 스택 비례 아츠 취약 지속 추가.\n" +
                       "TODO: 지속 구역 — Blob 연동 후 구현";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                for (Char ch : Actor.chars()) {
                    if (ch == hero || ch.alignment == Char.Alignment.ALLY) continue;
                    if (!ch.isAlive()) continue;
                    if (!hero.fieldOfView[ch.pos]) continue;

                    // 자연 피해
                    int dmg = Math.round(hero.damageRoll() * ULT_MULT);
                    ch.damage(dmg, hero, DamageType.NATURE);

                    if (!ch.isAlive()) continue;

                    // 자연 부착
                    ArtsAttachment.apply(ch, ArtsAttachment.ArtsType.NATURE, hero);

                    // 감속
                    Buff.affect(ch, Cripple.class, Cripple.DURATION);

                    // 아츠 취약 (방어불능 스택 비례 추가 지속)
                    DefenselessStack ds = ch.buff(DefenselessStack.class);
                    int stacks = (ds != null) ? ds.stacks() : 0;
                    float vulnDur = ArtsVulnerable.DURATION + stacks * ULT_STACK_BONUS;
                    Buff.affect(ch, ArtsVulnerable.class, vulnDur);
                }
                // TODO: 구역 지속 Blob 생성
            }
        };
    }
}
