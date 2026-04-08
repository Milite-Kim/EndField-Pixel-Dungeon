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
 * 늑대의 발톱 (WolfClaw) — 로시(Rosi)의 배틀스킬 조건부 적 디버프.
 *
 * [매 턴] 물리 피해(Dungeon.hero.damageRoll() × TICK_DMG_MULT)
 * [피해 증폭] 이 버프가 붙은 적이 받는 물리/열기 피해를 DMG_AMP_MULT 배 증가
 *             → Char.damage() 내 WolfClaw 훅이 자동 적용.
 *             단, WolfClaw 자신의 틱 피해는 증폭 제외 (자기 순환 방지).
 *
 * 적용: WolfClaw.apply(enemy, duration)
 * 피해 증폭 훅: Char.damage() 내에서 자동 처리 — 별도 호출 불필요.
 */
public class WolfClaw extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 매 턴 물리 틱 피해 배율 (hero.damageRoll() 기준). TODO: 수치 확정 */
    public static final float TICK_DMG_MULT = 0.3f;

    /**
     * 물리/열기 피해 증폭 배율.
     * 1.25 = 25% 추가 피해. TODO: 수치 확정
     */
    public static final float DMG_AMP_MULT = 1.25f;

    /** 기본 지속 시간 (턴). TODO: 수치 확정 */
    public static final int DEFAULT_DURATION = 4;

    // ─────────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────────

    private int duration = DEFAULT_DURATION;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    /**
     * 적에게 늑대의 발톱을 부여한다.
     * 이미 붙어 있으면 남은 시간을 갱신(더 긴 값으로).
     */
    public static void apply(Char enemy, int durationTurns) {
        WolfClaw buff = Buff.affect(enemy, WolfClaw.class);
        buff.duration = Math.max(buff.duration, durationTurns);
    }

    // ─────────────────────────────────────────────
    // 틱 동작
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        // 매 턴 물리 피해 (자기 자신이 src = WolfClaw 인스턴스 → 피해 증폭 대상에서 제외됨)
        if (target.isAlive() && Dungeon.hero != null) {
            int dmg = Math.round(Dungeon.hero.damageRoll() * TICK_DMG_MULT);
            target.damage(dmg, this, DamageType.PHYSICAL);
        }

        duration--;
        if (duration <= 0 || !target.isAlive()) {
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
    public int icon() {
        return BuffIndicator.NONE; // TODO: 전용 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(duration);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String DURATION = "duration";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(DURATION, duration);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        duration = bundle.getInt(DURATION);
    }
}
