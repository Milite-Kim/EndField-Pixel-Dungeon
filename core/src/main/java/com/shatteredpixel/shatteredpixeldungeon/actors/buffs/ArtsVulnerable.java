/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 아츠 취약 (ArtsVulnerable) — 탕탕 배틀스킬 / 안탈 배틀스킬 디버프
 *
 * 적에게 부여되면 받는 아츠 피해(열기/냉기/자연/전기)가 DMG_MULT배 증가.
 * Char.damage() 에서 DamageType.isArts() == true 시 체크.
 *
 * Electrified와의 차이:
 *   Electrified: 아츠 반응(전기+타속성)으로 발동, 소모 스택 비례 배율
 *   ArtsVulnerable: 탕탕/안탈 스킬 직접 부여, 고정 배율
 *
 * 사용법:
 *   ArtsVulnerable.apply(enemy);
 */
public class ArtsVulnerable extends FlavourBuff {

    { type = buffType.NEGATIVE; announced = true; }

    /** 지속 시간 (턴). TODO: 수치 확정 */
    public static final float DURATION = 5f;

    /** 아츠 피해 증폭 배율. TODO: 수치 확정 */
    public static final float DMG_MULT = 1.25f;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    public static void apply(Char enemy) {
        Buff.affect(enemy, ArtsVulnerable.class, DURATION);
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 아츠 취약 아이콘
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }
}
