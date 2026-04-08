/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 물리 취약 (PhysicalVulnerable) — 에스텔라 연계기 디버프
 *
 * 적에게 부여되면 받는 물리 피해가 DMG_MULT배 증가.
 * Char.damage() 에서 DamageType.PHYSICAL 시 체크.
 *
 * ArmorBreaked와의 차이:
 *   ArmorBreaked: DefenselessStack ARMOR_BREAK 소모 시 부여, 스택 비례 배율
 *   PhysicalVulnerable: 에스텔라 쇄빙 연계기에서 부여, 고정 배율
 *
 * 사용법:
 *   PhysicalVulnerable.apply(enemy);
 */
public class PhysicalVulnerable extends FlavourBuff {

    { type = buffType.NEGATIVE; announced = true; }

    /** 지속 시간 (턴). TODO: 수치 확정 */
    public static final float DURATION = 5f;

    /** 물리 피해 증폭 배율. TODO: 수치 확정 */
    public static final float DMG_MULT = 1.25f;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    public static void apply(Char enemy) {
        Buff.affect(enemy, PhysicalVulnerable.class, DURATION);
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 물리 취약 아이콘
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
