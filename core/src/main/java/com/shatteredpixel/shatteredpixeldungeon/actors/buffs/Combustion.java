/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 연소 (Combustion) - 열기 아츠 반응
 *
 * 타속성 부착 적에게 HEAT 부착 시 발동.
 * 소모된 아츠 스택 수에 비례한 열기 피해를 매 턴 고정 수치로 가한다.
 * (지속 시간이 줄어도 피해량은 변하지 않음)
 *
 * 기본 지속: {@link #BASE_DURATION}턴 (= {@link #BASE_DURATION}틱).
 * 오퍼레이터 특성·승급으로 damageMult / durationBonus / onTickExtra() 확장 가능.
 *
 * 기존 SPD의 Burning과 별개의 독립 클래스.
 */
public class Combustion extends Buff {

    {
        type = buffType.NEGATIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 기본 수치 상수
    // ─────────────────────────────────────────────

    /** 기본 지속 턴 (틱). TODO: 수치 확정 시 변경 */
    public static final int BASE_DURATION = 5;

    // ─────────────────────────────────────────────
    // 인스턴스 필드
    // ─────────────────────────────────────────────

    /** 아츠 반응 시 소모된 부착 스택 수 — 틱당 피해 기준값 */
    private int consumedStacks = 1;

    /** 남은 지속 턴 */
    private int duration = 0;

    /**
     * 틱당 피해 배율.
     * 오퍼레이터 특성·승급으로 증가 가능 (기본값 1.0f = 100%).
     */
    private float damageMult = 1f;

    /**
     * 지속 턴 보너스.
     * 오퍼레이터 특성·승급으로 추가 턴 부여 가능 (기본값 0).
     */
    private int durationBonus = 0;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    /**
     * 연소 부착 또는 갱신.
     * 이미 걸려 있는 연소보다 소모 스택이 같거나 클 때 갱신.
     * (더 약한 연소는 기존 연소를 덮어쓰지 않음)
     */
    public static void apply(Char enemy, int consumedStacks) {
        Combustion buff = Buff.affect(enemy, Combustion.class);
        if (consumedStacks >= buff.consumedStacks) {
            buff.consumedStacks = consumedStacks;
            buff.duration = BASE_DURATION + buff.durationBonus;
        }
    }

    // ─────────────────────────────────────────────
    // 피해 계산
    // ─────────────────────────────────────────────

    /**
     * 틱당 열기 피해.
     * consumedStacks 에 비례하며 남은 duration 과 무관하게 고정.
     *
     * TODO: 실제 피해 수식 확정 필요.
     *       현재는 스택 비례 임시값 사용.
     */
    private int damagePerTick() {
        // TODO: 수치 확정 — 스택 비례 기준값 설계 후 교체
        int base = consumedStacks; // 임시값
        return Math.round(base * damageMult);
    }

    // ─────────────────────────────────────────────
    // 확장 포인트: 오퍼레이터 특성·승급 전용
    // ─────────────────────────────────────────────

    /**
     * 틱당 피해 배율 설정.
     * 오퍼레이터 특성이나 승급에서 호출해 연소 피해를 강화.
     *
     * @param mult 1.0f = 기본, 1.5f = 50% 증가
     */
    public void setDamageMult(float mult) {
        this.damageMult = mult;
    }

    /**
     * 지속 턴 보너스 설정.
     * apply() 이전에 설정해야 반영됨.
     *
     * @param bonus 추가 턴 수
     */
    public void setDurationBonus(int bonus) {
        this.durationBonus = bonus;
    }

    /**
     * 매 틱 추가 효과 훅.
     * 오퍼레이터 특성·승급으로 연소에 추가 효과를 붙일 때 오버라이드.
     * (예: 연소 중인 적 공격 시 추가 피해, 상태이상 연계 등)
     *
     * 기본 구현은 아무것도 하지 않음.
     */
    protected void onTickExtra(Char target) {
        // 오버라이드 포인트 — 기본은 빈 구현
    }

    // ─────────────────────────────────────────────
    // 매 턴 동작
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        if (!target.isAlive()) {
            detach();
            return true;
        }

        if (duration > 0) {
            // 열기 피해 적용 (DamageType.HEAT → 감전 배율 미적용, 아츠 아이콘 표시)
            target.damage(damagePerTick(), this, DamageType.HEAT);
            duration--;
            spend(TICK);

            // 추가 효과 (오퍼레이터 특성·승급 확장 포인트)
            onTickExtra(target);
        } else {
            detach();
        }
        return true;
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 연소 전용 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(duration);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String CONSUMED_STACKS = "consumedStacks";
    private static final String DURATION        = "duration";
    private static final String DAMAGE_MULT     = "damageMult";
    private static final String DURATION_BONUS  = "durationBonus";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CONSUMED_STACKS, consumedStacks);
        bundle.put(DURATION,        duration);
        bundle.put(DAMAGE_MULT,     damageMult);
        bundle.put(DURATION_BONUS,  durationBonus);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        consumedStacks = bundle.getInt(CONSUMED_STACKS);
        duration       = bundle.getInt(DURATION);
        damageMult     = bundle.getFloat(DAMAGE_MULT);
        durationBonus  = bundle.getInt(DURATION_BONUS);
    }
}
