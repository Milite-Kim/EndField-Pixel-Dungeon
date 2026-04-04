/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.operators.team;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;

/**
 * 진천우 (Jincheonwoo)
 *
 * 직군: 가드
 * 무기: 한손검
 * 속성: 물리
 *
 * [배틀스킬] 물리 피해 + 띄우기
 * [연계기]
 *   - 조건: 적 방어불능 스택 누적 시
 *   - 효과: 관통 베기 → 물리 피해 + 띄우기 (관통 이동 가능)
 * [궁극기] 7회 연속 대량 물리 피해
 */
public class Jincheonwoo extends TeamOperator {

    @Override
    public String name() {
        return "진천우";
    }

    @Override
    public OperatorClass operatorClass() {
        return OperatorClass.GUARD;
    }

    @Override
    public WeaponType weaponType() {
        return WeaponType.ONE_HANDED_SWORD;
    }

    @Override
    public Attribute attribute() {
        return Attribute.PHYSICAL;
    }

    @Override
    public int baseCooldown() {
        // 연계기 쿨타임 (추후 수치 확정 시 변경)
        return 3;
    }

    /**
     * 연계기 조건: 적에게 방어불능 스택이 누적되어 있을 때
     *
     * TODO: 방어불능 스택 시스템이 구현되면 실제 체크 로직으로 교체
     */
    @Override
    public boolean chainCondition(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return false;

        // 임시: 방어불능 스택 버프가 구현되면 아래처럼 교체
        // return target.buff(DefenseBreakStack.class) != null;
        return false;
    }

    /**
     * 연계기 효과: 관통 베기 → 물리 피해 + 띄우기
     * 관통 이동 가능 (적을 통과하면서 이동)
     *
     * TODO: 관통 이동 및 물리 피해 로직 구현
     */
    @Override
    public void activateChain(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;

        // TODO: 관통 베기 애니메이션 및 물리 피해 처리
        // TODO: 띄우기(AirborneDebuff) 적용
        // TODO: 관통 이동 처리

        resetCooldown();
    }
}
