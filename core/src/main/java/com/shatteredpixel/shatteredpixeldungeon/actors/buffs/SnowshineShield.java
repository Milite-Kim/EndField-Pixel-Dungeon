/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 스노우샤인 쉴드 (SnowshineShield) — 스노우샤인 연계기 버프
 *
 * 스노우샤인 연계기 발동 시 Hero에게 부여하는 한시적 쉴드.
 * 카치르의 Barrier보다 쉴드량이 낮은 대신, 지속 종료 시 잔여 쉴드를 HP로 회복.
 *
 * [동작]
 * - duration 턴 동안 유지 (소모되어 0이 되거나 duration 만료 시 종료)
 * - 종료 시 잔여 쉴드 × RECOVERY_RATIO 만큼 HP 회복
 *
 * ShieldBuff를 상속하여 SPD의 피해 흡수 파이프라인과 자동 통합됨.
 * (ShieldBuff.processDamage()가 모든 ShieldBuff 서브클래스를 처리)
 */
public class SnowshineShield extends ShieldBuff {

    { type = buffType.POSITIVE; announced = true; }

    /** 지속 종료 시 잔여 쉴드 → HP 전환 비율. TODO: 수치 확정 */
    public static final float RECOVERY_RATIO = 0.50f;

    /** 쉴드 지속 시간 (턴). TODO: 수치 확정 */
    public static final int DURATION = 8;

    private int remaining = DURATION;

    // ─────────────────────────────────────────────
    // 적용 메서드
    // ─────────────────────────────────────────────

    public static void apply(Hero hero, int shieldAmount) {
        SnowshineShield buff = Buff.affect(hero, SnowshineShield.class);
        buff.incShield(shieldAmount); // 새로 설정 (중복 적용 시 합산)
        buff.remaining = DURATION;
    }

    // ─────────────────────────────────────────────
    // 매 턴: 지속 감소, 만료 시 잔여 쉴드 → HP 전환
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        remaining--;
        if (remaining <= 0 || shielding() <= 0) {
            expireWithRecovery();
        } else {
            spend(TICK);
        }
        return true;
    }

    /**
     * 지속 종료 시 잔여 쉴드를 HP로 전환하고 detach.
     */
    private void expireWithRecovery() {
        int leftover = shielding();
        if (leftover > 0 && Dungeon.hero != null && target == Dungeon.hero) {
            Hero hero = Dungeon.hero;
            int recover = Math.round(leftover * RECOVERY_RATIO);
            if (recover > 0) {
                hero.HP = Math.min(hero.HP + recover, hero.HT);
                hero.sprite.showStatus(CharSprite.POSITIVE, "+" + recover);
            }
        }
        detach();
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 스노우샤인 쉴드 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(remaining);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String REMAINING = "remaining";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(REMAINING, remaining);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        remaining = bundle.getInt(REMAINING);
    }
}
