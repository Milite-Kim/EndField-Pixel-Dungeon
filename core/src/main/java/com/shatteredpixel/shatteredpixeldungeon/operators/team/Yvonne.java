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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frozen;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.utils.PathFinder;

/**
 * 이본 (Yvonne)
 *
 * 직군: 스트라이커
 * 무기: 권총
 * 속성: 냉기
 *
 * [배틀스킬] 지정 위치에 냉기 장치 설치
 *             3×3 범위 즉시 냉기 피해 + 냉기 부착.
 *             TODO: 매 턴 냉기 충격파 + 종료 시 폭발(강제 동결) — Blob 시스템 연동 후 구현
 * [연계기]   조건: 적이 냉기 부착 or 자연 부착 보유 시
 *             효과: 냉기 피해 + 모든 아츠 부착 소모 → 강제 동결 + 스택 비례 추가 냉기 피해 + 궁극기 충전
 * [궁극기]   다음 5회 공격 강화 (자기 강화형, 셀프 타겟)
 *             TODO: 공속 단축, 동결 적 추가 피해, 5번째 강화 — 강화 버프 시스템 완성 후 구현
 */
public class Yvonne extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해/수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 단일 타격 냉기 피해 배율. TODO: 수치 확정 */
    private static final float SKILL_MULT = 0.6f;

    /** 연계기 기본 냉기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_BASE_MULT = 0.8f;

    /** 연계기 소모 스택당 추가 냉기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_STACK_MULT = 0.4f;

    /** 궁극기 강화 공격 횟수. */
    private static final int ULT_HIT_COUNT = 5;

    /** 궁극기 강화 공격 냉기 추가 피해 배율. TODO: 수치 확정 */
    private static final float ULT_HIT_MULT = 0.5f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override public String name()              { return "이본"; }
    @Override public OperatorClass operatorClass() { return OperatorClass.STRIKER; }
    @Override public WeaponType weaponType()    { return WeaponType.HANDGUN; }
    @Override public Attribute attribute()      { return Attribute.COLD; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 냉기 장치 설치
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override public int baseCooldown() { return 5; } // TODO: 수치 확정
            @Override public String name()        { return "냉기 장치 설치"; }
            @Override public String description() {
                return "지정 위치 3×3 범위 냉기 피해 + 냉기 부착.\n" +
                       "TODO: 매 턴 충격파 + 종료 시 강제 동결 폭발";
            }

            @Override public int range()          { return 5; } // 권총: 넓은 사거리
            @Override public boolean canTargetCell() { return true; } // 지면 타겟팅 가능

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // 3×3 범위(NEIGHBOURS9 = center + 8방향) 모든 적에게 냉기 피해 + 냉기 부착
                for (int offset : PathFinder.NEIGHBOURS9) {
                    int c = cell + offset;
                    if (c < 0 || c >= Dungeon.level.length()) continue;

                    Char ch = Actor.findChar(c);
                    if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) continue;

                    int dmg = Math.round(hero.damageRoll() * SKILL_MULT);
                    ch.damage(dmg, hero, DamageType.COLD);

                    if (ch.isAlive()) {
                        ArtsAttachment.apply(ch, ArtsAttachment.ArtsType.CRYO, hero);
                    }
                }
                // TODO: 장치 Actor 배치 → 매 턴 충격파 + n턴 후 폭발(강제 동결)
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 냉기 폭발
    // ─────────────────────────────────────────────

    @Override public int baseCooldown() { return 4; } // TODO: 수치 확정

    @Override public String chainName()        { return "냉기 폭발"; }
    @Override public String chainDescription() {
        return "조건: 적 냉기 or 자연 부착 보유 시\n" +
               "효과: 냉기 피해 + 부착 전량 소모 → 강제 동결 + 스택 비례 추가 냉기 피해 + 궁극기 충전";
    }

    /**
     * 연계기 조건: 적이 냉기 or 자연 부착을 보유 중일 때.
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        return arts != null
                && (arts.currentType() == ArtsAttachment.ArtsType.CRYO
                    || arts.currentType() == ArtsAttachment.ArtsType.NATURE);
    }

    /** 연계기 효과: 냉기 피해 + 부착 전량 소모 → 강제 동결 + 스택 비례 추가 냉기 피해 + 궁극기 충전 */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        ArtsAttachment arts = target.buff(ArtsAttachment.class);
        int consumedStacks = (arts != null) ? arts.stacks() : 0;

        // 기본 냉기 피해
        int dmg = Math.round(hero.damageRoll() * CHAIN_BASE_MULT);
        target.damage(dmg, hero, DamageType.COLD);

        if (!target.isAlive()) return;

        // 부착 전량 소모
        if (arts != null) arts.detach();

        // 강제 동결 (소모 스택 비례)
        if (consumedStacks > 0) {
            Frozen.apply(target, consumedStacks);
        }

        // 스택 비례 추가 냉기 피해
        if (consumedStacks > 0 && target.isAlive()) {
            int bonusDmg = Math.round(hero.damageRoll() * CHAIN_STACK_MULT * consumedStacks);
            target.damage(bonusDmg, hero, DamageType.COLD);
        }

        // 궁극기 충전
        if (hero.activeUltimate != null) {
            hero.activeUltimate.addChargeInternal(20); // TODO: 충전량 수치 확정
        }
    }

    // ─────────────────────────────────────────────
    // 궁극기: 강화 사격 모드 (자기 강화형)
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override public int maxCharge() { return 100; } // TODO: 수치 확정
            @Override public String name()   { return "강화 사격 모드"; }
            @Override public String description() {
                return "다음 " + ULT_HIT_COUNT + "회 공격에 냉기 추가 피해(×" + ULT_HIT_MULT + ") 자동 부여.\n" +
                       "TODO: 공속 단축, 동결 적 추가 피해, 5번째 타격 대폭 강화";
            }

            @Override public boolean selfTarget() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                // TODO: YvonneEnhanced 버프 적용 (남은 강화 공격 횟수 추적)
                // 현재는 플레이스홀더: 즉시 냉기 피해 5연타 (가장 가까운 적에게)
                Char nearest = null;
                int minDist = Integer.MAX_VALUE;
                for (Char ch : Actor.chars()) {
                    if (ch == hero || ch.alignment == Char.Alignment.ALLY) continue;
                    if (!ch.isAlive()) continue;
                    int d = Dungeon.level.distance(hero.pos, ch.pos);
                    if (d < minDist) { minDist = d; nearest = ch; }
                }
                if (nearest != null) {
                    for (int i = 0; i < ULT_HIT_COUNT; i++) {
                        if (!nearest.isAlive()) break;
                        int dmg = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                        nearest.damage(dmg, hero, DamageType.COLD);
                    }
                }
            }
        };
    }
}
