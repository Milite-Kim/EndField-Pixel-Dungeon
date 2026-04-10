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
 * 침식된 기질 (ErodedTrait) — 저주받은 무기에 대응하는 기질.
 *
 * 장착 시 해제 불가 (cursed = true).
 * 부정적 효과와 약한 긍정 효과를 동시에 보유할 수 있다.
 *
 * ※ 상세 설계 미확정 — 저주받은 무기 시스템 분석 후 결정.
 *    현재는 구조적 플레이스홀더만 정의.
 *
 * TODO: 침식된 기질 공통 효과 (부정적 proc, 패시브 디버프 등) 구현
 */
public abstract class ErodedTrait extends Trait {

    {
        cursed      = true;  // 항상 저주 상태 → 장착 후 해제 불가
        cursedKnown = true;  // 저주 여부를 처음부터 식별
    }

    @Override
    public int tier() { return 1; } // 서브클래스에서 오버라이드

    @Override
    public int requiredStat() { return 10; } // 서브클래스에서 오버라이드
}
