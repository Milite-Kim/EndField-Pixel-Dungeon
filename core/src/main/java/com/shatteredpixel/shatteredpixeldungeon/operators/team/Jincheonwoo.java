/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DefenselessStack;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.watabou.noosa.Game;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;

/**
 * 진천우 (Jincheonwoo)
 *
 * 직군: 가드
 * 무기: 한손검
 * 속성: 물리
 *
 * [배틀스킬] 섬광 베기 — 물리 피해(×SKILL_MULT) + 띄우기(LAUNCH)
 * [연계기]   관통 베기 — 조건: 방어불능 스택 보유 시
 *             효과: 물리 피해(×CHAIN_MULT) + LAUNCH
 *             추가: 진천우가 메인 오퍼레이터일 때 → 관통 이동 (적 뒤로 이동)
 * [궁극기]   절공 — 7회 연속 물리 피해(×ULT_HIT_MULT)
 *             TODO: 피해 수치 확정
 *             TODO: 승급 — 도중 처치 시 주변 적으로 연장 / 처치 비례 충전 반환
 */
public class Jincheonwoo extends TeamOperator {

    // ─────────────────────────────────────────────
    // 피해 배율 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 배틀스킬 피해 배율 (hero.damageRoll() 기준). TODO: 수치 확정 */
    private static final float SKILL_MULT    = 1.5f;

    /** 연계기 피해 배율. TODO: 수치 확정 */
    private static final float CHAIN_MULT    = 1.2f;

    /** 궁극기 히트 1회당 피해 배율. TODO: 수치 확정 */
    private static final float ULT_HIT_MULT  = 0.8f;

    /** 궁극기 총 히트 수 */
    private static final int   ULT_HIT_COUNT = 7;

    /** 궁극기 타격 사이 딜레이 (초). 애니메이션 후 다음 타격 시작 전 정지 시간. TODO: 수치 확정 */
    private static final float HIT_INTERVAL  = 0.12f;

    // ─────────────────────────────────────────────
    // 오퍼레이터 기본 정보
    // ─────────────────────────────────────────────

    @Override
    public String name() { return "진천우"; }

    @Override
    public OperatorClass operatorClass() { return OperatorClass.GUARD; }

    @Override
    public WeaponType weaponType() { return WeaponType.ONE_HANDED_SWORD; }

    @Override
    public Attribute attribute() { return Attribute.PHYSICAL; }

    // ─────────────────────────────────────────────
    // 배틀스킬: 섬광 베기
    // ─────────────────────────────────────────────

    @Override
    public BattleSkill battleSkill() {
        return new BattleSkill() {

            @Override
            public int baseCooldown() {
                return 4; // TODO: 수치 확정
            }

            @Override
            public String name() { return "섬광 베기"; }

            @Override
            public String description() {
                return "물리 피해(" + SKILL_MULT + "×) + 띄우기(LAUNCH).\nTODO: 피해 수치 확정";
            }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) return;

                int damage = Math.round(hero.damageRoll() * SKILL_MULT);
                target.damage(damage, hero, DamageType.PHYSICAL);

                if (target.isAlive()) {
                    DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
                }
            }
        };
    }

    // ─────────────────────────────────────────────
    // 연계기: 관통 베기
    // ─────────────────────────────────────────────

    @Override
    public int baseCooldown() {
        return 3; // TODO: 수치 확정
    }

    @Override
    public String chainName() { return "관통 베기"; }

    @Override
    public String chainDescription() {
        return "조건: 적 방어불능 스택 보유 시\n" +
               "효과: 물리 피해 + LAUNCH\n" +
               "진천우 메인 시 추가: 관통 이동";
    }

    /** 연계기 조건: 적에게 방어불능 스택이 있을 때 */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;
        return target.buff(DefenselessStack.class) != null;
    }

    /**
     * 연계기 효과: 물리 피해 + LAUNCH.
     * 진천우가 메인 오퍼레이터일 때만 관통 이동(적 뒤로 이동)을 수행한다.
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // 물리 피해
        int damage = Math.round(hero.damageRoll() * CHAIN_MULT);
        target.damage(damage, hero, DamageType.PHYSICAL);

        // 띄우기
        if (target.isAlive()) {
            DefenselessStack.apply(target, DefenselessStack.PhysicalAbnormality.LAUNCH, hero);
        }

        // 관통 이동: 진천우가 메인 오퍼레이터일 때만
        if (hero.activeMainOperator instanceof Jincheonwoo) {
            penetrate(hero, target);
        }
    }

    /**
     * 관통 이동: 영웅이 적을 통과해 적 뒤쪽 1칸으로 이동한다.
     * 목표 셀이 이동 불가하거나 다른 캐릭터가 있으면 이동하지 않는다.
     *
     * 방향: hero → enemy → (enemy + delta)
     */
    private void penetrate(Hero hero, Char target) {
        int width = Dungeon.level.width();
        int dx = Integer.signum((target.pos % width) - (hero.pos % width));
        int dy = Integer.signum((target.pos / width) - (hero.pos / width));

        if (dx == 0 && dy == 0) return;

        int destCell = target.pos + dy * width + dx;
        if (destCell < 0 || destCell >= Dungeon.level.length()) return;
        if (!Dungeon.level.passable[destCell]) return;
        if (Actor.findChar(destCell) != null) return;

        // travelling=false: 순간이동형 이동 (발소리/Vertigo 없음)
        hero.sprite.move(hero.pos, destCell);
        hero.move(destCell, false);
        Dungeon.observe(); // 시야 갱신
    }

    // ─────────────────────────────────────────────
    // 궁극기: 절공
    // ─────────────────────────────────────────────

    @Override
    public Ultimate ultimate() {
        return new Ultimate() {

            @Override
            public int maxCharge() {
                return 100; // TODO: 수치 확정
            }

            @Override
            public String name() { return "절공"; }

            @Override
            public String description() {
                return ULT_HIT_COUNT + "회 연속 물리 피해(" + ULT_HIT_MULT + "×/회).\n" +
                       "TODO: 피해 수치 확정\n" +
                       "TODO(승급): 도중 처치 시 주변 적으로 연장 / 처치 비례 충전 반환";
            }

            @Override
            public boolean isAnimated() { return true; }

            @Override
            protected void activate(Hero hero, Char target, int cell) {
                if (target == null || !target.isAlive()) {
                    hero.spend(castTime());
                    hero.next();
                    return;
                }
                applyNextHit(hero, target, ULT_HIT_COUNT);
            }

            /**
             * 재귀 콜백 체인: 애니메이션 1회 → 피해 적용 → HIT_INTERVAL 딜레이 → 다음 타격
             *
             * sprite.attack() 콜백 → 피해 → Tweener(HIT_INTERVAL) → applyNextHit 재귀
             * Tweener는 렌더 스레드에서 실제 경과 시간을 재며, 완료 시 다음 타격을 시작한다.
             */
            private void applyNextHit(final Hero hero, final Char target, final int hitsLeft) {
                if (hitsLeft <= 0 || !target.isAlive()) {
                    hero.spend(castTime());
                    hero.next();
                    return;
                }

                hero.sprite.attack(target.pos, new Callback() {
                    @Override
                    public void call() {
                        if (target.isAlive()) {
                            int damage = Math.round(hero.damageRoll() * ULT_HIT_MULT);
                            target.damage(damage, hero, DamageType.PHYSICAL);
                        }

                        // 타격 후 짧은 정지 → 다음 타격으로 리듬감 부여
                        Tweener pause = new Tweener(hero.sprite, HIT_INTERVAL) {
                            @Override
                            protected void updateValues(float progress) { }

                            @Override
                            protected void onComplete() {
                                super.onComplete();
                                applyNextHit(hero, target, hitsLeft - 1);
                            }
                        };
                        Game.scene().add(pause);
                    }
                });
            }
        };
    }
}
