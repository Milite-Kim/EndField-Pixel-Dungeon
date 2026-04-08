/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 아케쿠리 궁극기 버프 — 소대 집합 (AkekuriUltimateBuff)
 *
 * 아케쿠리 궁극기 발동 시 Hero에게 부여.
 *
 * [즉시 효과 — 궁극기 activate()에서 처리]
 *   팀 오퍼레이터 전원 연계기 쿨타임 즉시 50% 감소 (TeamOperator.reduceCooldownByHalf())
 *
 * [지속 효과 — 이 버프 act()에서 처리]
 *   매 턴 누적 0.3씩 감소치를 쌓아, 1.0 이상이 되면 팀 오퍼레이터 전원 쿨타임 1 추가 감소.
 *   (= 대략 매 3.3턴마다 팀 전체 쿨타임 1 추가 단축 ≈ 30% 가속 효과)
 *
 * TODO: 수치 확정 (DURATION, EXTRA_REDUCTION_PER_TURN)
 */
public class AkekuriUltimateBuff extends Buff {

    { type = buffType.POSITIVE; announced = true; }

    // ─────────────────────────────────────────────
    // 수치 상수 (TODO: 수치 확정)
    // ─────────────────────────────────────────────

    /** 지속 시간 (턴). TODO: 수치 확정 */
    public static final int DURATION = 8;

    /**
     * 매 턴 누적되는 추가 쿨타임 감소량 (소수).
     * 0.3f → 약 3.3턴마다 쿨타임 1 추가 감소 (30% 가속).
     * TODO: 수치 확정
     */
    public static final float EXTRA_REDUCTION_PER_TURN = 0.3f;

    // ─────────────────────────────────────────────
    // 상태 필드
    // ─────────────────────────────────────────────

    private int   remainingTurns = DURATION;
    private float accum          = 0f;

    // ─────────────────────────────────────────────
    // 매 턴 처리
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        accum += EXTRA_REDUCTION_PER_TURN;
        if (accum >= 1.0f) {
            int extra = (int) accum;
            accum -= extra;
            // 팀 오퍼레이터 전원 쿨타임 추가 감소
            Hero hero = (Hero) target;
            for (TeamOperator op : hero.teamOperators) {
                op.forceCooldownReduction(extra);
            }
        }

        remainingTurns--;
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

    private static final String REMAINING = "remainingTurns";
    private static final String ACCUM     = "accum";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(REMAINING, remainingTurns);
        bundle.put(ACCUM,     accum);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        remainingTurns = bundle.getInt(REMAINING);
        accum          = bundle.getFloat(ACCUM);
    }
}
