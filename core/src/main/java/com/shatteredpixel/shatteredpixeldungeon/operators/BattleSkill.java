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
 * 배틀스킬 추상 베이스 클래스.
 *
 * 모든 오퍼레이터는 배틀스킬 데이터를 보유하나,
 * 실제 사용은 메인 오퍼레이터로 운용할 때만 가능.
 *
 * [공통 동작]
 * - 쿨타임: 매 턴 1씩 감소, 0이 되면 사용 가능
 * - 사용: use() 호출 → activate() 실행 → 쿨타임 리셋
 *
 * [타겟팅 흐름]
 * - 버튼 첫 번째 클릭: Hero.enterBattleSkillTargeting() 호출 → 타겟팅 모드 진입
 *   - 이미 타겟팅 모드 + 공격 대상 존재 → 즉시 발동 (더블클릭 효과)
 * - 타겟 셀 클릭: Hero.confirmBattleSkillTarget(cell) 호출
 * - 사거리 초과:
 *   - autoApproach() == true  → 대상에게 접근 후 자동 발동
 *   - autoApproach() == false → "대상이 너무 멀리 떨어져 있습니다" 후 취소
 *
 * [서브클래스 구현 의무]
 * - baseCooldown() : 오퍼레이터별 기본 쿨타임 (수치는 추후 확정)
 * - activate()     : 실제 스킬 효과
 *
 * [선택적 오버라이드]
 * - range()        : 스킬 사거리 (기본 1칸, 원거리 스킬은 오버라이드)
 * - castTime()     : 발동 턴 소모 (기본 1턴)
 * - autoApproach() : 사거리 초과 시 자동 접근 여부 (기본 false)
 * - canTargetCell() : 빈 지면 타겟팅 가능 여부 (기본 false, 설치형/지면형은 true)
 *
 * [로스터 패턴별 TODO 분류]
 * - 물리/아츠 피해 수치       → 피해 수치 시스템 확정 후
 * - 궁극기 충전               → Ultimate 시스템 완성 후
 * - 방어/패링 (카치크 등)     → 피격 처리 훅 완성 후
 * - 소환/설치형 (이본 등)     → Blob/지형 시스템 연동 후
 * - 자버프 (라스트 라이트 등) → 해당 오퍼레이터 전용 버프 구현 후
 */
public abstract class BattleSkill implements Bundlable {

    // 현재 쿨타임 (0 = 사용 가능)
    private int cooldown = 0;

    // ─────────────────────────────────────────────
    // 서브클래스에서 반드시 구현해야 하는 것들
    // ─────────────────────────────────────────────

    /**
     * 오퍼레이터별 기본 쿨타임 (턴 단위).
     * TODO: 각 오퍼레이터 구현 시 수치 확정
     */
    public abstract int baseCooldown();

    /**
     * 스킬 실제 효과.
     * use()에서 내부적으로 호출됨.
     *
     * @param hero   메인 오퍼레이터 (플레이어)
     * @param target 현재 공격 대상 (canTargetCell()==true인 스킬은 null 가능)
     * @param cell   타겟 셀 위치 (canTargetCell()==true인 스킬에서 사용)
     */
    protected abstract void activate(Hero hero, Char target, int cell);

    // ─────────────────────────────────────────────
    // 선택적 오버라이드 — 스킬 특성 정의
    // ─────────────────────────────────────────────

    /**
     * 스킬 사거리 (셀 거리 기준).
     * 기본값 1 = 근접 (바로 인접한 칸).
     * 원거리 스킬은 오버라이드하여 더 높은 값 반환.
     */
    public int range() {
        return 1;
    }

    /**
     * 발동 시 소모하는 게임 시간.
     * 기본값 1f = 표준 1턴.
     * 특수 스킬(빠른 시전, 느린 시전)은 오버라이드.
     */
    public float castTime() {
        return 1f;
    }

    /**
     * 사거리 초과 시 자동 접근 여부.
     * true  → 대상에게 이동한 후 자동 발동
     * false → "대상이 너무 멀리 떨어져 있습니다" 메시지 후 취소 (기본값)
     */
    public boolean autoApproach() {
        return false;
    }

    /**
     * 빈 지면(Char 없는 셀) 타겟팅 가능 여부.
     * false → Char 타겟 전용 (기본값)
     * true  → 설치형/지면 폭발형 스킬 (이본 냉기 장치 등)
     */
    public boolean canTargetCell() {
        return false;
    }

    // ─────────────────────────────────────────────
    // 스킬 사용 (외부에서 이것만 호출)
    // ─────────────────────────────────────────────

    /**
     * 배틀스킬 사용.
     * 쿨타임 중이면 아무것도 하지 않음.
     * Hero.actBattleSkill()에서 범위 체크 후 호출됨.
     *
     * @param hero   플레이어
     * @param target 타겟 Char (canTargetCell 스킬의 지면 타겟팅 시 null)
     * @param cell   타겟 셀
     */
    public void use(Hero hero, Char target, int cell) {
        if (!isReady()) return;
        activate(hero, target, cell);
        cooldown = baseCooldown();
    }

    // ─────────────────────────────────────────────
    // 쿨타임 관리
    // ─────────────────────────────────────────────

    /** 매 턴 호출 — 쿨타임 1씩 감소 */
    public void reduceCooldown() {
        if (cooldown > 0) cooldown--;
    }

    /** 사용 가능 여부 */
    public boolean isReady() {
        return cooldown == 0;
    }

    /** 현재 쿨타임 조회 (UI 표시용) */
    public int cooldown() {
        return cooldown;
    }

    /**
     * 쿨타임 강제 감소 (자이히 충전 효과 등 특수 케이스용).
     * @param amount 감소량 (음수 불가)
     */
    public void reduceCooldownBy(int amount) {
        cooldown = Math.max(0, cooldown - amount);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String COOLDOWN = "cooldown";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(COOLDOWN, cooldown);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        cooldown = bundle.getInt(COOLDOWN);
    }
}
