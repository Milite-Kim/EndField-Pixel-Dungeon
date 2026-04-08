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
 * Hero의 ChainQueue에 등록되어 UI 버튼이 나타납니다.
 * 플레이어가 버튼을 누르면 activateChain()이 호출됩니다.
 *
 * 연계기 큐 타이머/순서는 Hero.chainQueue 에서 관리합니다.
 * 각 오퍼레이터는 이 클래스를 상속받아 구현합니다.
 * 예) Jincheonwoo extends TeamOperator
 */
public abstract class TeamOperator extends Operator {

    // 연계기 현재 쿨타임 (0이면 발동 가능)
    // protected: 아크라이트처럼 쿨타임 로직을 오버라이드하는 서브클래스가 직접 접근
    protected int cooldown = 0;

    // ─────────────────────────────────────────────
    // 서브클래스에서 반드시 구현해야 하는 것들
    // ─────────────────────────────────────────────

    /**
     * 연계기 발동 기본 쿨타임 (턴 단위).
     * 각 오퍼레이터마다 다르게 설정합니다.
     */
    public abstract int baseCooldown();

    /**
     * 연계기 이름 (UI 표시용).
     * 예) "관통 베기", "아츠 결정 투척"
     */
    public abstract String chainName();

    /**
     * 연계기 설명 (UI 표시용).
     * 조건 및 효과를 포함해서 서술.
     * 예) "조건: 적 방어불능 스택 누적 시\n효과: 물리 피해 + 관통 이동"
     */
    public abstract String chainDescription();

    /**
     * 연계기 큐 진입 조건 체크.
     * 특정 게임 이벤트(강력한 일격, DefenselessStack 부여 등) 발생 시
     * Hero가 이 메서드를 호출하여 조건이 충족됐는지 확인합니다.
     * true를 반환하면 Hero.chainQueue에 이 오퍼레이터가 추가됩니다.
     *
     * @param hero   메인 오퍼레이터(플레이어)
     * @param target 현재 공격 대상 (없으면 null)
     */
    public abstract boolean chainCondition(Hero hero, Char target);

    /**
     * 연계기 실제 효과.
     * 플레이어가 UI 버튼을 눌러 발동했을 때 호출됩니다.
     *
     * @param hero   메인 오퍼레이터(플레이어)
     * @param target 현재 공격 대상 (없으면 null)
     */
    public abstract void activateChain(Hero hero, Char target);

    // ─────────────────────────────────────────────
    // 쿨타임 관리 (공통 로직)
    // ─────────────────────────────────────────────

    /** 연계기 발동 가능 여부 (쿨타임이 0인 경우) */
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

    /**
     * 쿨타임을 즉시 절반으로 줄인다.
     * 아케쿠리 궁극기 "소대 집합" 즉시 효과에서 호출.
     */
    public void reduceCooldownByHalf() {
        cooldown = cooldown / 2;
    }

    /**
     * 쿨타임을 지정 값만큼 강제로 줄인다 (최솟값 0).
     * 아케쿠리 궁극기 지속 효과(AkekuriUltimateBuff)에서 호출.
     *
     * @param amount 줄일 쿨타임 턴 수
     */
    public void forceCooldownReduction(int amount) {
        cooldown = Math.max(0, cooldown - amount);
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
