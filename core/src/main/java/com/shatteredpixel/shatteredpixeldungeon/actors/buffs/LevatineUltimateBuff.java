/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 레바테인 궁극기 강화 버프 (LevatineUltimateBuff)
 *
 * 레바테인 궁극기 "용광로의 불꽃" 발동 시 Hero에게 부여.
 *
 * [지속 효과]
 *   - 데미지 배율 증가 (DMG_MULT)
 *   - 매 FLAME_INTERVAL턴마다 녹아내린 불꽃(MoltenFlame) 1스택 자동 획득
 *   - 배틀스킬 1회 강화 준비 (skillEnhancedReady = true)
 *     → 다음 배틀스킬 발동 시 강화 모드(4스택 소모 분기)로 강제 진입
 *
 * [사거리 +1칸]
 *   TODO: Hero 공격 범위 시스템 구현 후 연동 (현재 미적용)
 *
 * [연동 클래스]
 *   Levatine.java — 배틀스킬에서 이 버프의 skillEnhancedReady / DMG_MULT를 읽음
 */
public class LevatineUltimateBuff extends Buff {

    { type = buffType.POSITIVE; announced = true; }

    // ─────────────────────────────────────────────
    // 수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 지속 시간 (턴). TODO: 수치 확정 */
    public static final int DURATION = 8;

    /** 녹아내린 불꽃 자동 획득 간격 (턴). TODO: 수치 확정 */
    public static final int FLAME_INTERVAL = 3;

    /** 데미지 증가 배율. TODO: 수치 확정 */
    public static final float DMG_MULT = 1.3f;

    // ─────────────────────────────────────────────
    // 상태 필드
    // ─────────────────────────────────────────────

    /** 남은 지속 시간 */
    private int remainingTurns = DURATION;

    /** 불꽃 자동 획득까지 남은 턴 */
    private int flameTick = FLAME_INTERVAL;

    /**
     * 배틀스킬 강화 준비 플래그.
     * true이면 다음 배틀스킬이 강화 모드(4스택 소모 분기)로 발동.
     * 강화 배틀스킬 발동 후 false로 초기화.
     */
    public boolean skillEnhancedReady = true;

    // ─────────────────────────────────────────────
    // 매 턴 처리
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        remainingTurns--;
        flameTick--;

        // 주기적 녹아내린 불꽃 자동 획득
        if (flameTick <= 0) {
            flameTick = FLAME_INTERVAL;
            MoltenFlame flame = target.buff(MoltenFlame.class);
            if (flame != null) flame.addStack();
        }

        if (remainingTurns <= 0) {
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
        return Integer.toString(remainingTurns);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String REMAINING       = "remainingTurns";
    private static final String FLAME_TICK      = "flameTick";
    private static final String SKILL_ENHANCED  = "skillEnhancedReady";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(REMAINING,      remainingTurns);
        bundle.put(FLAME_TICK,     flameTick);
        bundle.put(SKILL_ENHANCED, skillEnhancedReady);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        remainingTurns     = bundle.getInt(REMAINING);
        flameTick          = bundle.getInt(FLAME_TICK);
        skillEnhancedReady = bundle.getBoolean(SKILL_ENHANCED);
    }
}
