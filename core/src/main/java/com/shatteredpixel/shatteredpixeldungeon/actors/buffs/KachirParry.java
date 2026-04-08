/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 카치르(Kachir)의 배틀스킬로 Hero에게 부여되는 패링 버프 (1회용).
 *
 * 다음 물리 공격을 완전 차단(100%)하고 카운터어택 실행:
 *   → 물리 피해(×COUNTER_MULT) + 방어불능 스택 1개 추가
 *
 * 아츠 피해 70% 감소: Hero.damage(int, Object, DamageType) 경로 연결 후 활성화.
 * TODO: 아츠 피해 감소는 DamageType-aware damage hook 구현 시 추가
 */
public class KachirParry extends Buff {

    {
        type = buffType.POSITIVE;
        announced = true;
    }

    /** 카운터 피해 배율. TODO: 수치 확정 */
    public static final float COUNTER_MULT = 0.8f;

    /**
     * Hero가 물리 공격을 받을 때 Hero.damage() 에서 호출.
     * 피해를 완전 차단하고 카운터어택을 실행한 뒤 버프를 소모한다.
     *
     * @param hero     패링 중인 영웅
     * @param attacker 공격한 적
     * @param dmg      (Hero의 각종 감소 계산 이후의) 최종 물리 피해량
     * @return 실제로 받을 피해량 — 물리 완전 차단이므로 항상 0 반환
     */
    public int interceptPhysical(Hero hero, Char attacker, int dmg) {
        // 카운터: 물리 피해
        int counterDmg = Math.round(hero.damageRoll() * COUNTER_MULT);
        attacker.damage(counterDmg, hero, DamageType.PHYSICAL);

        // 카운터: 방어불능 스택 1개 추가 (이상 발동 없음)
        if (attacker.isAlive()) {
            DefenselessStack.addStackOnly(attacker);
        }

        // 1회 소모
        detach();

        return 0; // 물리 피해 완전 차단
    }

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 전용 아이콘
    }
}
