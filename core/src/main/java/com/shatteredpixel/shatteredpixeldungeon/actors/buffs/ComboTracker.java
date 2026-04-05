/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 기본 공격 콤보 트래커.
 *
 * Hero가 공격할 때마다 단계(step)가 올라가며,
 * 마지막 단계 = 강력한 일격 (높은 피해 배율 + 연계기 트리거).
 *
 * 비활성 타이머: 일정 턴 동안 공격이 없으면 콤보 초기화.
 * (기본값 {@link #INACTIVITY_TIMEOUT}턴. TODO: 수치 확정)
 *
 * 콤보 배율 패턴은 메인 오퍼레이터의 {@code comboMultipliers()}에서 가져옴.
 * 메인 오퍼레이터 시스템 구현 전까지는 기본값(한손검 3단계) 사용.
 */
public class ComboTracker extends Buff {

    {
        type = buffType.POSITIVE;
        announced = false;
    }

    /** 공격 없이 이 턴 수가 지나면 콤보 초기화. TODO: 3~5 중 확정 필요 */
    public static final int INACTIVITY_TIMEOUT = 4;

    /** 현재 콤보 단계 (0부터 시작) */
    private int step = 0;

    /** 비활성 카운트다운 (0이 되면 콤보 초기화) */
    private int inactivityTimer = INACTIVITY_TIMEOUT;

    /** 직전 공격이 강력한 일격이었는지 (연계기 조건 체크용) */
    private boolean lastWasFinishingBlow = false;

    // ─────────────────────────────────────────────
    // 공격 전: 현재 단계 배율 조회
    // ─────────────────────────────────────────────

    /**
     * 현재 콤보 단계의 피해 배율.
     * Hero.onAttackComplete()에서 attack() 호출 직전에 사용.
     */
    public float currentMultiplier(Hero hero) {
        float[] pattern = getPattern(hero);
        if (step >= pattern.length) return 1f;
        return pattern[step];
    }

    /**
     * 현재 단계가 강력한 일격(마지막 단계)인지 여부.
     * 연계기 조건 판정에 사용.
     */
    public boolean isFinishingBlow(Hero hero) {
        float[] pattern = getPattern(hero);
        return step == pattern.length - 1;
    }

    // ─────────────────────────────────────────────
    // 공격 후: 단계 전진
    // ─────────────────────────────────────────────

    /**
     * 공격 후 콤보 단계를 전진시킨다.
     * 강력한 일격 이후에는 단계를 0으로 초기화.
     *
     * @return 이번 공격이 강력한 일격이었으면 true
     */
    public boolean advanceStep(Hero hero) {
        float[] pattern = getPattern(hero);
        lastWasFinishingBlow = (step == pattern.length - 1);

        if (lastWasFinishingBlow) {
            step = 0; // 강력한 일격 후 콤보 처음부터
        } else {
            step++;
        }

        // 공격했으므로 비활성 타이머 리셋
        inactivityTimer = INACTIVITY_TIMEOUT;
        spend(TICK); // 다음 act() 예약

        return lastWasFinishingBlow;
    }

    /** 직전 공격이 강력한 일격이었는지 (연계기 조건 체크 등에서 사용) */
    public boolean wasLastFinishingBlow() {
        return lastWasFinishingBlow;
    }

    /** 현재 콤보 단계 (UI 표시용) */
    public int step() {
        return step;
    }

    // ─────────────────────────────────────────────
    // 콤보 패턴 조회
    // ─────────────────────────────────────────────

    private float[] getPattern(Hero hero) {
        // TODO: 메인 오퍼레이터 시스템 완성 후
        //       hero.activeMainOperator.comboMultipliers() 로 교체

        // activeWeaponType 이 설정되어 있으면 무기 유형별 기본 패턴 사용
        if (hero.activeWeaponType != null) {
            switch (hero.activeWeaponType) {
                case TWO_HANDED_SWORD:
                    return new float[]{ 1.0f, 1.4f };    // 2단계: 중 → 강력한 일격
                case POLEARM:
                    return new float[]{ 0.8f, 1.0f, 1.3f };
                case HANDGUN:
                    return new float[]{ 0.8f, 0.9f, 1.2f };
                case ARTS_UNIT:
                    return new float[]{ 0.7f, 0.8f, 1.1f };
                case ONE_HANDED_SWORD:
                default:
                    return new float[]{ 0.8f, 0.9f, 1.2f };
            }
        }

        // 기본값 (activeWeaponType 미설정 시): 한손검 3단계
        return new float[]{ 0.8f, 0.9f, 1.2f };
    }

    // ─────────────────────────────────────────────
    // 비활성 타이머 처리 (매 턴)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        inactivityTimer--;
        if (inactivityTimer <= 0) {
            // 너무 오래 공격 안 함 → 콤보 초기화
            step = 0;
            lastWasFinishingBlow = false;
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
        return BuffIndicator.NONE; // TODO: 콤보 전용 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(step + 1); // 1단계부터 표시
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String STEP             = "step";
    private static final String INACTIVITY_TIMER = "inactivityTimer";
    private static final String LAST_FINISHING   = "lastWasFinishingBlow";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STEP,             step);
        bundle.put(INACTIVITY_TIMER, inactivityTimer);
        bundle.put(LAST_FINISHING,   lastWasFinishingBlow);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        step             = bundle.getInt(STEP);
        inactivityTimer  = bundle.getInt(INACTIVITY_TIMER);
        lastWasFinishingBlow = bundle.getBoolean(LAST_FINISHING);
    }
}
