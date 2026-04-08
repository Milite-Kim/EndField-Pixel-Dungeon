/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 적의 차지(차지 공격 예비 동작) 상태를 나타내는 마커 버프.
 *
 * 사용법 (엔드필드 전용 몬스터 AI에서):
 *   차지 시작 시: Charging.startCharge(enemy);
 *   차지 종료 시: Buff.detach(enemy, Charging.class);
 *
 * 카치르(Kachir)의 연계기 조건 '적 차지 시작 시'가 이 버프를 감지한다.
 */
public class Charging extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = false; // UI 아이콘 미표시 — 별도 몬스터 스프라이트로 표현
    }

    /**
     * 적의 차지를 시작하고 팀 연계기 조건 체크를 트리거한다.
     * 엔드필드 전용 몬스터 AI에서 차지 공격 준비 시 호출.
     */
    public static void startCharge(Char enemy) {
        Buff.affect(enemy, Charging.class);
        if (Dungeon.hero != null) {
            Dungeon.hero.checkChainTriggers(enemy);
        }
    }

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE;
    }
}
