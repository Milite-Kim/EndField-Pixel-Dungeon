/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 강력한 썬더랜스 (StrongThunderLance) — 아비웨나 궁극기 전용 부착형 버프
 *
 * ThunderLance와 동일한 부착/폭발 구조이지만 피해 배율이 훨씬 높다.
 * 아비웨나 궁극기 적중 시 1개 부착.
 *
 * [회수 피해 배율]  RECALL_DMG_MULT × count
 * [자동 폭발 배율] AUTO_DMG_MULT × count
 */
public class StrongThunderLance extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 자동 폭발까지 남은 턴. TODO: 수치 확정 */
    public static final int AUTO_DETONATE_TURNS = 4;

    /** 배틀스킬 회수 시 강력한 랜스 1개당 피해 배율. TODO: 수치 확정 */
    public static final float RECALL_DMG_MULT = 1.5f;

    /** 자동 폭발 시 강력한 랜스 1개당 피해 배율. TODO: 수치 확정 */
    public static final float AUTO_DMG_MULT = 1.0f;

    // ─────────────────────────────────────────────
    // 상태 필드
    // ─────────────────────────────────────────────

    private int count = 0;
    private int timer = 0;

    // ─────────────────────────────────────────────
    // 외부 호출
    // ─────────────────────────────────────────────

    /**
     * 적에게 N개의 강력한 썬더랜스를 부착한다. 타이머를 리셋.
     */
    public static void attach(Char enemy, int lances) {
        StrongThunderLance buff = Buff.affect(enemy, StrongThunderLance.class);
        buff.count += lances;
        buff.timer = AUTO_DETONATE_TURNS;
    }

    /** 현재 부착된 강력한 랜스 수. */
    public int count() { return count; }

    // ─────────────────────────────────────────────
    // 버프 동작: 자동 폭발 타이머
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        if (!target.isAlive()) {
            detach();
            return true;
        }
        timer--;
        if (timer <= 0) {
            // 자동 폭발
            if (count > 0 && Dungeon.hero != null) {
                int dmg = Math.round(Dungeon.hero.damageRoll() * AUTO_DMG_MULT * count);
                target.damage(dmg, Dungeon.hero, DamageType.ELECTRIC);
            }
            detach();
        } else {
            spend(TICK);
        }
        return true;
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    @Override
    public int icon() { return BuffIndicator.NONE; } // TODO: 강력한 썬더랜스 아이콘

    @Override
    public String iconTextDisplay() { return Integer.toString(count); }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String COUNT = "count";
    private static final String TIMER = "timer";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COUNT, count);
        bundle.put(TIMER, timer);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        count = bundle.getInt(COUNT);
        timer = bundle.getInt(TIMER);
    }
}
