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
 * 메인 오퍼레이터는 런 시작 시 확정되며 런 중 변경되지 않는다.
 * 따라서 activate() 호출 시점(장착/로드)에 isForCurrentMain() 체크 한 번으로 충분하다.
 *
 * TODO: 오퍼레이터당 1종씩 items/traits/special/ 에 구현
 */
public abstract class SpecialTrait extends Trait {

    /** 특수 기질은 항상 5티어. */
    @Override
    public final int tier() { return 5; }

    /** 5티어 기본 요구 능력치. */
    @Override
    public int requiredStat() { return 18; }

    /** 5티어 기본 ATK 보너스 (CommonTrait T5와 동일). */
    @Override
    public int traitATK() { return 180; }

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
