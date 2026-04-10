/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.traits;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;

/**
 * 특수 기질 (SpecialTrait) — 특정 오퍼레이터가 메인일 때 고유 효과를 발휘하는 5티어 기질.
 *
 * - 귀속 오퍼레이터가 메인일 때 : operatorActivate() / operatorDeactivate() / operatorProc() 호출
 * - 다른 오퍼레이터가 메인일 때 : 5티어 공용 기질처럼 기본 효과만 적용
 *
 * 효과 설계 패턴 (docs/엔픽던_기질시스템.md):
 *   특수 기질 로직은 해당 오퍼레이터 클래스 안에 위임하는 방식을 권장.
 *   SpecialTrait 서브클래스는 뼈대를 갖고, 실제 효과는 operatorClass() 지정 오퍼레이터에서 처리.
 *
 * 오퍼레이터 전환 시 주의:
 *   메인 오퍼레이터가 바뀌면 Hero.syncActiveOperator() → Operator.onBecomeMain() 이 호출되므로,
 *   필요 시 해당 훅에서 hero.belongings.trait 의 activate/deactivate 를 재호출해야 한다.
 *   (TODO: onBecomeMain 훅 연동)
 *
 * TODO: 오퍼레이터당 1종씩 items/traits/special/ 에 구현
 */
public abstract class SpecialTrait extends Trait {

    /** 특수 기질은 항상 5티어. */
    @Override
    public final int tier() { return 5; }

    /** 5티어 기본 요구 능력치. TODO: 수치 확정 */
    @Override
    public int requiredStat() { return 18; }

    // ─────────────────────────────────────────────
    // 귀속 오퍼레이터
    // ─────────────────────────────────────────────

    /**
     * 이 특수 기질의 귀속 오퍼레이터 클래스.
     * 해당 오퍼레이터가 메인일 때만 특수 효과가 활성화된다.
     */
    public abstract Class<? extends Operator> operatorClass();

    /** 현재 Hero의 메인 오퍼레이터가 귀속 오퍼레이터인지 확인. */
    public boolean isForCurrentMain(Hero hero) {
        return hero.activeMainOperator != null
                && operatorClass().isInstance(hero.activeMainOperator);
    }

    // ─────────────────────────────────────────────
    // 특수 효과 진입점 (서브클래스 또는 오퍼레이터에서 구현)
    // ─────────────────────────────────────────────

    /**
     * 귀속 오퍼레이터 메인 시 장착/로드 때 호출되는 활성화 훅.
     * 기본값: no-op
     */
    public void operatorActivate(Hero hero) {}

    /**
     * 귀속 오퍼레이터 메인 시 해제 때 호출되는 비활성화 훅.
     * 기본값: no-op
     */
    public void operatorDeactivate(Hero hero) {}

    /**
     * 귀속 오퍼레이터 메인 시 공격 명중 proc.
     * SpecialTrait.proc() 내부에서 isForCurrentMain() 확인 후 호출.
     * 기본값: damage 그대로 반환
     */
    public int operatorProc(Char attacker, Char defender, int damage) {
        return damage;
    }

    // ─────────────────────────────────────────────
    // Trait 훅 위임
    // ─────────────────────────────────────────────

    @Override
    public void activate(Hero hero) {
        if (isForCurrentMain(hero)) {
            operatorActivate(hero);
        }
    }

    @Override
    public void deactivate(Hero hero) {
        if (isForCurrentMain(hero)) {
            operatorDeactivate(hero);
        }
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero && isForCurrentMain((Hero) attacker)) {
            return operatorProc(attacker, defender, damage);
        }
        return damage;
    }
}
