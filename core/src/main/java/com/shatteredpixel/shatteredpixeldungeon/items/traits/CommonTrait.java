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

/**
 * 공용 기질 (CommonTrait) — 모든 오퍼레이터가 장착 가능한 범용 기질.
 *
 * 티어 1~5에 걸쳐 존재하며, 오퍼레이터 고유 조건 없이 기본 옵션만 제공한다.
 *
 * 티어별 기본 요구 능력치 (TODO: 수치 확정):
 *   1티어 STR 10 / 2티어 STR 12 / 3티어 STR 14 / 4티어 STR 16 / 5티어 STR 18
 *
 * 효과 구현 방식 (서브클래스에서 선택):
 *   지속 패시브 — activate(Hero) 에서 버프 부여, deactivate(Hero) 에서 제거
 *   공격 프록   — proc(Char, Char, int) 오버라이드
 *
 * TODO: 구체적인 공용 기질 종류 설계 후 items/traits/common/ 에 각 클래스 추가
 */
public abstract class CommonTrait extends Trait {

    // ─────────────────────────────────────────────
    // 티어별 기본 요구 능력치 (확정)
    // ─────────────────────────────────────────────

    private static final int[] REQUIRED_STAT_BY_TIER = { 10, 12, 14, 16, 18 };

    // ─────────────────────────────────────────────
    // 티어별 기본 ATK 보너스 (확정)
    //   T1 혼탁: 50  → STR 10 기준 총 ATK 60 (max 피해 6)
    //   T2 안정: 75  → ATK 85  (max 8)
    //   T3 세련: 105 → ATK 115 (max 11)
    //   T4 순수: 140 → ATK 150 (max 15)
    //   T5 무결: 180 → ATK 190 (max 19)
    // ─────────────────────────────────────────────

    private static final int[] ATK_BY_TIER = { 50, 75, 105, 140, 180 };

    // ─────────────────────────────────────────────
    // 등급 필드
    // ─────────────────────────────────────────────

    private final int tierValue;

    protected CommonTrait(int tier) {
        this.tierValue = tier;
    }

    @Override
    public int tier() {
        return tierValue;
    }

    /**
     * 기본 요구 능력치 — 티어에 따라 자동 결정.
     * 특수한 요구치가 필요한 경우 서브클래스에서 오버라이드.
     */
    @Override
    public int requiredStat() {
        int idx = Math.max(0, Math.min(tierValue - 1, REQUIRED_STAT_BY_TIER.length - 1));
        return REQUIRED_STAT_BY_TIER[idx];
    }

    /**
     * 티어별 기본 ATK 보너스.
     * 수식어 기질은 이 값에 추가 효과를 더하는 방식으로 구현.
     */
    @Override
    public int traitATK() {
        int idx = Math.max(0, Math.min(tierValue - 1, ATK_BY_TIER.length - 1));
        return ATK_BY_TIER[idx];
    }
}
