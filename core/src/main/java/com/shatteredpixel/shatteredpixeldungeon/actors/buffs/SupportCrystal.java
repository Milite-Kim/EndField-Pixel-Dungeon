/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 지원 결정체 (SupportCrystal) — 자이히 배틀스킬 버프
 *
 * 자이히 배틀스킬 사용 시 Hero에게 부여.
 * 강력한 일격(onFinishingBlowLanded) 적중 시 발동:
 *   → 추가 냉기 피해(×COLD_MULT) + 아츠 취약(ArtsVulnerable) 부여
 *   최대 MAX_HITS회 발동 후 자동 소멸. 재사용 시 갱신(남은 횟수 초기화).
 *
 * onPowerfulHit()은 Hero.onFinishingBlowLanded()에서 호출됨.
 * 버프가 없으면 즉시 반환하므로 다른 오퍼레이터에게 영향 없음.
 *
 * NOTE: "아츠 취약"은 ArtsVulnerable 버프로 표현 (아직 미구현 → 플레이스홀더로 냉기 피해만 추가).
 * TODO: ArtsVulnerable 구현 후 onPowerfulHit에서 apply 호출 추가
 */
public class SupportCrystal extends Buff {

    { type = buffType.POSITIVE; announced = true; }

    /** 강력한 일격 당 추가 냉기 피해 배율. TODO: 수치 확정 */
    public static final float COLD_MULT = 0.5f;

    /** 최대 발동 횟수. 재부여 시 갱신. */
    public static final int MAX_HITS = 3;

    private int hitsLeft = MAX_HITS;

    // ─────────────────────────────────────────────
    // 적용 메서드 (갱신 포함)
    // ─────────────────────────────────────────────

    public static void apply(Hero hero) {
        SupportCrystal crystal = Buff.affect(hero, SupportCrystal.class);
        crystal.hitsLeft = MAX_HITS; // 재부여 시 횟수 갱신
    }

    public int hitsLeft() { return hitsLeft; }

    // ─────────────────────────────────────────────
    // 강력한 일격 발동 훅
    // ─────────────────────────────────────────────

    /**
     * 강력한 일격 적중 시 호출.
     * 결정체가 있으면 냉기 피해 + 아츠 취약 부여 후 카운트 감소.
     */
    public static void onPowerfulHit(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;
        SupportCrystal crystal = hero.buff(SupportCrystal.class);
        if (crystal == null) return;

        // 추가 냉기 피해
        int dmg = Math.round(hero.damageRoll() * COLD_MULT);
        target.damage(dmg, hero, DamageType.COLD);

        // TODO: ArtsVulnerable.apply(target) — 구현 완료 후 추가

        crystal.hitsLeft--;
        if (crystal.hitsLeft <= 0) {
            crystal.detach();
        }
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (카운트 소진까지 영구)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 지원 결정체 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(hitsLeft);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String HITS_LEFT = "hitsLeft";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(HITS_LEFT, hitsLeft);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        hitsLeft = bundle.getInt(HITS_LEFT);
    }
}
