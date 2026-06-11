/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * 고통 (AnguishBuff) — 공용 기질 '고통'(AnguishTrait)이 부여하는 자기 강화 버프.
 *
 * 궁극기 사용 후 일정 턴 동안 공격력(ATK)이 ATK_MULT 배 증가한다.
 * 적용 지점: Hero.getATK() (결전 준비형 효과).
 *
 * TODO: ATK_MULT / DURATION 수치 확정
 */
public class AnguishBuff extends FlavourBuff {

    /** 공격력 증가 배율 (ATK × 이 값). TODO: 수치 확정 */
    public static final float ATK_MULT = 1.3f;

    public static final float DURATION = 5f;

    {
        type = buffType.POSITIVE;
    }

    @Override
    public int icon() {
        return BuffIndicator.RAGE;
    }

    @Override
    public void tintIcon(Image icon) {
        icon.hardlight(1f, 0.3f, 0.3f);
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
