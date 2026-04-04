/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.operators;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;

/**
 * 팀 오퍼레이터 추상 클래스.
 *
 * 팀 오퍼레이터는 Hero가 장착하는 "특수 슬롯" 개념입니다.
 * 화면에 직접 등장하지 않고, 연계기 조건이 충족되면
 * UI 버튼이 나타나 플레이어가 연계기를 발동할 수 있습니다.
 *
 * 각 오퍼레이터는 이 클래스를 상속받아 구현합니다.
 * 예) Jincheonwoo extends TeamOperator
 */
public abstract class TeamOperator extends Operator {

    // 연계기 현재 쿨타임 (0이면 발동 가능)
    private int cooldown = 0;

    // ─────────────────────────────────────────────
    // 서브클래스에서 반드시 구현해야 하는 것들
    // ─────────────────────────────────────────────

    /**
     * 연계기 발동 기본 쿨타임 (턴 단위).
     * 각 오퍼레이터마다 다르게 설정합니다.
     */
    public abstract int baseCooldown();

    /**
     * 연계기 발동 조건 체크.
     * 조건이 true 이면 UI에 연계기 버튼이 표시됩니다.
     *
     * @param hero   메인 오퍼레이터(플레이어)
     * @param target 현재 공격 대상 (없으면 null)
     */
    public abstract boolean chainCondition(Hero hero, Char target);

    /**
     * 연계기 실제 효과.
     * 플레이어가 버튼을 눌렀을 때 호출됩니다.
     *
     * @param hero   메인 오퍼레이터(플레이어)
     * @param target 현재 공격 대상 (없으면 null)
     */
    public abstract void activateChain(Hero hero, Char target);

    // ─────────────────────────────────────────────
    // 쿨타임 관리 (공통 로직)
    // ─────────────────────────────────────────────

    /** 연계기 발동 가능 여부 */
    public boolean isReady() {
        return cooldown == 0;
    }

    /** 매 턴 호출 - 쿨타임 1씩 감소 */
    public void reduceCooldown() {
        if (cooldown > 0) cooldown--;
    }

    /** 연계기 사용 후 쿨타임 초기화 */
    public void resetCooldown() {
        cooldown = baseCooldown();
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String COOLDOWN = "cooldown";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COOLDOWN, cooldown);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        cooldown = bundle.getInt(COOLDOWN);
    }
}
