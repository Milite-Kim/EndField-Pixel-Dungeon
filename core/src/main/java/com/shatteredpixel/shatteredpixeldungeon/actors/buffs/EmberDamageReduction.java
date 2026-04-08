/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 엠버 피해 감소 버프 (EmberDamageReduction)
 *
 * 엠버 배틀스킬 사용 시 Hero에게 1턴간 부여.
 * 해당 턴 동안 받는 모든 피해를 REDUCTION 비율만큼 감소.
 *
 * Hero.damage()에서 참조하여 적용.
 *
 * TODO: 피해 감소율 수치 확정
 */
public class EmberDamageReduction extends FlavourBuff {

    { type = buffType.POSITIVE; announced = true; }

    /** 피해 감소 비율 (0.3f = 30% 감소). TODO: 수치 확정 */
    public static final float REDUCTION = 0.3f;

    /** 지속 시간: 1턴 */
    public static final float DURATION = TICK;

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 전용 아이콘
    }
}
