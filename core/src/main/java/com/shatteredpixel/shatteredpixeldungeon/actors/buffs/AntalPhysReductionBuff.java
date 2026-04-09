/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 안탈 물리 피해 감소 버프 (AntalPhysReductionBuff)
 *
 * 안탈 배틀스킬 사용 시 Hero에게 부여.
 * 아츠유닛 충전 소모량 비례 지속 시간 동안 받는 물리 피해 감소.
 *
 * Hero.damage(int, Object)에서 참조하여 적용.
 * (물리 공격은 2인수 damage() 경로를 통해 Hero에 전달됨)
 *
 * 지속 시간: Antal.battleSkill.activate()에서 Buff.affect(hero, this, reductionTurns)로 설정.
 *
 * TODO: 피해 감소율 수치 확정
 */
public class AntalPhysReductionBuff extends FlavourBuff {

    { type = buffType.POSITIVE; announced = true; }

    /** 물리 피해 감소 비율 (0.25f = 25% 감소). TODO: 수치 확정 */
    public static final float REDUCTION = 0.25f;

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 전용 아이콘
    }

    @Override
    public float iconFadePercent() {
        // visualcooldown()은 Buff.affect에서 지정한 duration 기준 자동 계산
        float max = Math.max(1f, cooldown());
        return Math.max(0f, 1f - visualcooldown() / max);
    }
}
