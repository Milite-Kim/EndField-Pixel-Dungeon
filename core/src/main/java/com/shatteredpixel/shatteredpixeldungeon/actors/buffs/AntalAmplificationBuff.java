/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 안탈 증폭 버프 (AntalAmplificationBuff) — 안탈 궁극기 지속 버프
 *
 * Hero에게 부여되는 버프로, 지속 시간 동안 두 가지 효과를 제공:
 *
 * ① 전기·열기 증폭 (amplMult)
 *    Hero 팀이 가하는 전기·열기 피해에 AMP_MULT 배율 추가.
 *    팀 연계기 포함 모든 전기·열기 피해 소스에 적용.
 *    → Char.damage()에서 src == Dungeon.hero 시 체크.
 *
 * ② 즉흥적인 천재성 (Improvised Genius)
 *    위 버프 지속 중 메인 오퍼레이터의 기본 공격 or 배틀스킬 전기·열기 적중 시
 *    Hero HP를 GENIUS_HEAL만큼 회복.
 *    팀 연계기 발동 중(Hero.chainActivationContext = true)에는 발동 안 함.
 *    → Char.damage()에서 체크. onNonChainHit()으로 회복 실행.
 */
public class AntalAmplificationBuff extends Buff {

    {
        type = buffType.POSITIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 버프 지속 시간 (턴). TODO: 수치 확정 */
    public static final int DURATION = 8;

    /** 전기·열기 피해 증폭 배율 (1.0 = 기본, 1.2 = 20% 증가). TODO: 수치 확정 */
    public static final float AMP_MULT = 1.2f;

    /** 즉흥적인 천재성: 1회 적중 시 회복 HP. TODO: 수치 확정 */
    public static final int GENIUS_HEAL = 3;

    // ─────────────────────────────────────────────
    // 상태 필드
    // ─────────────────────────────────────────────

    private int remainingTurns = DURATION;

    // ─────────────────────────────────────────────
    // 버프 동작
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    // 효과 메서드
    // ─────────────────────────────────────────────

    /**
     * ① 전기·열기 피해 증폭 배율. Char.damage()에서 호출.
     */
    public float amplMult() {
        return AMP_MULT;
    }

    /**
     * ② 즉흥적인 천재성 회복. Char.damage()에서 팀 연계기가 아닌 히트 시 호출.
     * target이 Hero인 경우에만 동작 (이 버프는 항상 Hero에게 부착됨).
     */
    public void onNonChainHit() {
        if (target != null && target.isAlive()) {
            target.HP = Math.min(target.HP + GENIUS_HEAL, target.HT);
            // TODO: 회복 플로팅 텍스트 표시
        }
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 안탈 증폭 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(remainingTurns);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

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
