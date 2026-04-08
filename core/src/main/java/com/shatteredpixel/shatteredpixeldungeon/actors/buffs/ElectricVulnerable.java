/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 전기 취약 (ElectricVulnerable) — 안탈 배틀스킬 디버프
 *
 * 적에게 부여되면 받는 전기 피해가 DMG_MULT배 증가.
 * Char.damage()에서 DamageType.ELECTRIC 시 체크.
 *
 * 사용법:
 *   ElectricVulnerable.apply(enemy);
 */
public class ElectricVulnerable extends FlavourBuff {

    { type = buffType.NEGATIVE; announced = true; }

    /** 지속 시간 (턴). TODO: 수치 확정 */
    public static final float DURATION = 5f;

    /** 전기 피해 증폭 배율. TODO: 수치 확정 */
    public static final float DMG_MULT = 1.25f;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    public static void apply(Char enemy) {
        Buff.affect(enemy, ElectricVulnerable.class, DURATION);
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 전기 취약 아이콘
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
