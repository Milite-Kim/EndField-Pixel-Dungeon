/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 아츠 증폭 (ArtsAmplification) — 아군 강화 버프
 *
 * Hero(메인 오퍼레이터)에게 부여되는 긍정 버프로, 지속 시간 동안
 * 플레이어 팀이 가하는 아츠(열기/냉기/자연/전기) 피해를 AMP_MULT 배 증폭한다.
 * → Char.damage()에서 type.isArts() && src == Dungeon.hero 일 때 적용.
 *
 * 자이히(Zaihe)의 지원 결정체(강력한 일격 시) 및 궁극기(냉기·자연 증폭)에서 부여한다.
 * 재부여 시 지속 시간이 갱신된다(중첩 없음).
 *
 * NOTE: 안탈(AntalAmplificationBuff)이 전기·열기 한정 증폭인 것과 달리,
 *       본 버프는 아츠 4속성 전체를 증폭하는 범용 아츠 증폭이다.
 */
public class ArtsAmplification extends Buff {

    {
        type = buffType.POSITIVE;
        announced = true;
    }

    /** 버프 지속 시간 (턴). TODO: 수치 확정 */
    public static final int DURATION = 6;

    /** 아츠 피해 증폭 배율 (1.0 = 기본, 1.2 = 20% 증가). TODO: 수치 확정 */
    public static final float AMP_MULT = 1.2f;

    private int remainingTurns = DURATION;

    /** Hero에게 아츠 증폭 부여 (재부여 시 지속 시간 갱신). */
    public static void apply(Hero hero) {
        ArtsAmplification buff = Buff.affect(hero, ArtsAmplification.class);
        buff.remainingTurns = DURATION;
    }

    /** 아츠 피해 증폭 배율. Char.damage()에서 호출. */
    public float amplMult() {
        return AMP_MULT;
    }

    @Override
    public boolean act() {
        remainingTurns--;
        if (remainingTurns <= 0) {
            detach();
        } else {
            spend(TICK);
        }
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 아츠 증폭 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(remainingTurns);
    }

    private static final String REMAINING = "remainingTurns";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(REMAINING, remainingTurns);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        remainingTurns = bundle.getInt(REMAINING);
    }
}
