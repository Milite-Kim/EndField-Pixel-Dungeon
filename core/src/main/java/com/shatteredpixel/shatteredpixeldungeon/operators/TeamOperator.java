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

    /**
     * 이벤트형 연계기 트리거 플래그.
     *
     * Hero에서 특정 이벤트(강력한 일격, 배틀스킬 적중 등) 발생 시
     * {@link #markChainReady(int)} 로 세팅된다.
     *
     * 이 플래그를 쓰는지 여부는 오퍼레이터별 {@code chainCondition()} 에서 결정.
     * (상태형 연계기는 이 플래그를 무시하고 게임 상태를 직접 체크)
     */
    private boolean chainReady      = false;
    private int     chainReadyTimer = 0;   // 남은 유효 턴 (0 되면 자동 소멸)

    /** 이벤트 발생 시 호출. validTurns 턴 동안 chainReady = true 유지. */
    public void markChainReady(int validTurns) {
        chainReady      = true;
        chainReadyTimer = validTurns;
    }

    /** 연계기 발동 후 또는 만료 시 플래그 해제. */
    public void clearChainReady() {
        chainReady      = false;
        chainReadyTimer = 0;
    }

    /** 이벤트형 연계기가 현재 유효한지 여부 (chainCondition 내부에서 사용). */
    public boolean isChainReady() {
        return chainReady;
    }

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

    /** 매 턴 호출 - 쿨타임 1씩 감소, chainReady 유효 턴 차감 */
    public void reduceCooldown() {
        if (cooldown > 0) cooldown--;
        if (chainReady) {
            chainReadyTimer--;
            if (chainReadyTimer <= 0) clearChainReady();
        }
    }

    /** 연계기 사용 후 쿨타임 초기화 */
    public void resetCooldown() {
        cooldown = baseCooldown();
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String COOLDOWN          = "cooldown";
    private static final String CHAIN_READY       = "chainReady";
    private static final String CHAIN_READY_TIMER = "chainReadyTimer";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COOLDOWN,          cooldown);
        bundle.put(CHAIN_READY,       chainReady);
        bundle.put(CHAIN_READY_TIMER, chainReadyTimer);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        cooldown        = bundle.getInt(COOLDOWN);
        chainReady      = bundle.getBoolean(CHAIN_READY);
        chainReadyTimer = bundle.getInt(CHAIN_READY_TIMER);
    }
}
