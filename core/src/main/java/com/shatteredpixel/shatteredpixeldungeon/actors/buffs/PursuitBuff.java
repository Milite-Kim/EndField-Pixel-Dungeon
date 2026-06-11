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
 * 추격 (PursuitBuff) — 공용 기질 '추격'(PursuitTrait)이 부여하는 버프.
 *
 * 보유 중인 동안 이동 속도가 SPEED_MULT 배 증가한다.
 * 적용 지점: Char.speed() (Haste와 동일 모델).
 *
 * TODO: SPEED_MULT / DURATION 수치 확정
 */
public class PursuitBuff extends FlavourBuff {

    /** 이동 속도 증가 배율. TODO: 수치 확정 */
    public static final float SPEED_MULT = 1.5f;

    public static final float DURATION = 3f;

    {
        type = buffType.POSITIVE;
    }

    @Override
    public int icon() {
        return BuffIndicator.HASTE;
    }

    @Override
    public void tintIcon(Image icon) {
        icon.hardlight(0.5f, 1f, 0.5f);
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
