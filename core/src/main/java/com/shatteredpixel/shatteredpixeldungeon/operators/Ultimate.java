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
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * 궁극기 추상 베이스 클래스.
 *
 * 배틀스킬(턴 기반 쿨타임)과 달리 궁극기는 이벤트 기반 충전 방식.
 * 충전량이 최대치에 도달하면 사용 가능.
 *
 * [충전 방법 - 두 종류]
 * - addCharge()         : 외부 충전 (배틀스킬, 연계기, 처치 등)
 *                         canReceiveExternalCharge()가 false면 무시됨
 * - addChargeInternal() : 내부 충전 (자체 연계기 등 오퍼레이터 고유 조건)
 *                         항상 적용됨 (라스트 라이트 등 특수 케이스 대응)
 *
 * [라스트 라이트 특수 케이스]
 * "타 오퍼레이터의 궁극기 충전 효과 적용 불가, 자체 연계기로만 충전"
 * → canReceiveExternalCharge()를 false로 오버라이드
 * → 자체 연계기에서는 addChargeInternal() 사용
 *
 * [로스터 패턴별 TODO 분류]
 * - 다단 히트 (진천우 7회, 아델리아 10회 등)  → 히트 시퀀스 시스템 완성 후
 * - 자기 강화형 (이본, 레바테인, 안탈)         → 해당 버프 구현 후
 * - 범위/지속형 (스노우샤인, 탕탕, 질베르타)   → Blob/지형 시스템 연동 후
 * - 피해 수치                                  → 수치 확정 후
 */
public abstract class Ultimate implements Bundlable {

    // 현재 충전량
    private int charge = 0;

    // ─────────────────────────────────────────────
    // 서브클래스에서 반드시 구현해야 하는 것들
    // ─────────────────────────────────────────────

    /**
     * 최대 충전량.
     * 오퍼레이터별로 다름 (펠리카처럼 낮게, 라스트 라이트처럼 높게 설정 가능).
     * TODO: 각 오퍼레이터 구현 시 수치 확정
     */
    public abstract int maxCharge();

    /**
     * 궁극기 실제 효과.
     * use()에서 내부적으로 호출됨.
     *
     * @param hero   메인 오퍼레이터 (플레이어)
     * @param target 공격 대상 (범위기/자기 강화형은 null 가능)
     */
    protected abstract void activate(Hero hero, Char target);

    // ─────────────────────────────────────────────
    // 이벤트별 충전량 (선택적 오버라이드)
    // 기본값 0 = 해당 이벤트로 충전되지 않음
    // 나중에 특성(Talent) 시스템에서 보정량을 addCharge()로 추가 가능
    // ─────────────────────────────────────────────

    /**
     * 강력한 일격 적중 시 충전량.
     * 기본 공격 콤보의 마지막 타격에서 발생.
     */
    public int chargePerFinishingBlow() {
        return 0;
    }

    /**
     * 배틀스킬 사용 시 충전량.
     * actBattleSkill()에서 스킬 발동 직후 호출.
     * 스킬 내부에서 상태 조건에 따라 충전하는 경우(포그라니치니크 등)는
     * activate() 안에서 addChargeInternal()로 별도 처리.
     */
    public int chargePerBattleSkill() {
        return 0;
    }

    /**
     * 연계기 발동 시 충전량.
     * activateFrontChain()에서 연계기 발동 직후 호출.
     */
    public int chargePerChain() {
        return 0;
    }

    // ─────────────────────────────────────────────
    // 외부 충전 차단 여부 (기본값: 허용)
    // ─────────────────────────────────────────────

    /**
     * 외부 이벤트(배틀스킬, 연계기, 처치 등)에 의한 충전 허용 여부.
     *
     * 라스트 라이트처럼 "자체 연계기로만 충전"인 경우 false로 오버라이드.
     * false로 설정해도 addChargeInternal()은 항상 동작.
     */
    public boolean canReceiveExternalCharge() {
        return true;
    }

    // ─────────────────────────────────────────────
    // 충전 메서드
    // ─────────────────────────────────────────────

    /**
     * 외부 충전 (배틀스킬 사용, 연계기 발동, 적 처치 등).
     * canReceiveExternalCharge()가 false면 무시됨.
     *
     * @param amount 충전량
     */
    public void addCharge(int amount) {
        if (!canReceiveExternalCharge()) return;
        charge = Math.min(charge + amount, maxCharge());
    }

    /**
     * 내부 충전 (오퍼레이터 자체 조건 달성 시).
     * canReceiveExternalCharge() 여부에 무관하게 항상 적용.
     *
     * 사용 예: 라스트 라이트 자체 연계기, 레바테인 녹아내린 불꽃 4스택 소모
     *
     * @param amount 충전량
     */
    public void addChargeInternal(int amount) {
        charge = Math.min(charge + amount, maxCharge());
    }

    // ─────────────────────────────────────────────
    // 사용 (외부에서 이것만 호출)
    // ─────────────────────────────────────────────

    /**
     * 궁극기 사용.
     * 충전 미완료 시 아무것도 하지 않음.
     */
    public void use(Hero hero, Char target) {
        if (!isReady()) return;
        activate(hero, target);
        charge = 0;
    }

    // ─────────────────────────────────────────────
    // 상태 조회
    // ─────────────────────────────────────────────

    /** 사용 가능 여부 (충전 완료) */
    public boolean isReady() {
        return charge >= maxCharge();
    }

    /** 현재 충전량 (UI 표시용) */
    public int charge() {
        return charge;
    }

    /** 충전 진행률 0.0~1.0 (게이지 UI용) */
    public float chargePercent() {
        return (float) charge / maxCharge();
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String CHARGE = "charge";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(CHARGE, charge);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        charge = bundle.getInt(CHARGE);
    }
}
