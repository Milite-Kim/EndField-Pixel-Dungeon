/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.operators;

/**
 * 연계기 트리거 이벤트 종류.
 *
 * 연계기는 **조건이 발생하는 순간**에만 트리거되어야 한다.
 * 예) 진천우는 "적에게 방어불능이 부착되는 순간"에만 발동해야지,
 *     적이 이미 방어불능 상태라는 이유로 기본 공격에까지 반응하면 안 된다.
 *
 * {@code Hero.checkChainTriggers(target, trigger)}가 이벤트 종류를 넘겨주고,
 * 각 오퍼레이터는 {@code TeamOperator.chainTriggers()}로 자신이 반응할 이벤트를 선언한다.
 * 선언하지 않은 이벤트에서는 {@code chainCondition()}이 아예 평가되지 않는다.
 *
 * ※ 쿨타임 중 발생한 이벤트는 {@code isReady()} 검사에서 이미 걸러지므로,
 *   "쿨타임이 끝나자마자 과거 조건으로 발동"하는 일도 함께 방지된다.
 */
public enum ChainTrigger {

    /** 물리 이상(띄우기/넘어뜨리기/강타/갑옷파괴) 적용 — {@code DefenselessStack.apply()} */
    PHYSICAL_ABNORMALITY,

    /** 아츠 부착/폭발/반응 발생 — {@code ArtsAttachment.apply()} */
    ARTS_ATTACH,

    /** 메인 오퍼레이터의 강력한 일격 적중 — {@code Hero.onFinishingBlowLanded()} */
    FINISHING_BLOW,

    /** 아군 연계기 적중 — {@code Hero.activateFrontChain()} */
    ALLY_CHAIN,

    /** 적이 차지(공격 준비)를 시작 — {@code Charging.startCharge()} */
    ENEMY_CHARGE,

    /** 적 피격 횟수 임계 도달 — {@code HitCounter} */
    HIT_COUNT,

    /** 감전 상태 소모 — 아크라이트 배틀스킬 */
    ELECTRIC_CONSUMED
}
