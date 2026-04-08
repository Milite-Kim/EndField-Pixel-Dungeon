/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 저온 주입 (LowTempInjection) — 라스트 라이트 배틀스킬 버프
 *
 * 라스트 라이트 배틀스킬 사용 시 Hero에게 부여.
 * 다음 강력한 일격(onFinishingBlowLanded) 적중 시 소모:
 *   → 추가 냉기 피해(×COLD_MULT) + 냉기 부착(CRYO) 부여
 *
 * 라스트 라이트의 배틀스킬은 턴 소모 없이 즉시 이 버프만 부여함.
 * (BattleSkill.castTime() = 0f 로 설정)
 *
 * tryConsume()은 Hero.onFinishingBlowLanded()에서 호출됨.
 * 버프가 없으면 즉시 반환하므로 다른 오퍼레이터에게 영향 없음.
 */
public class LowTempInjection extends Buff {

    { type = buffType.POSITIVE; announced = true; }

    /** 소모 시 추가 냉기 피해 배율. TODO: 수치 확정 */
    public static final float COLD_MULT = 0.8f;

    // ─────────────────────────────────────────────
    // 강력한 일격 소모 훅
    // ─────────────────────────────────────────────

    /**
     * 강력한 일격 적중 시 호출.
     * 버프가 있으면 소모 → 냉기 피해 + 냉기 부착.
     */
    public static void tryConsume(Hero hero, Char target) {
        if (target == null || !target.isAlive()) return;
        LowTempInjection injection = hero.buff(LowTempInjection.class);
        if (injection == null) return;

        injection.detach(); // 1회 소모

        int dmg = Math.round(hero.damageRoll() * COLD_MULT);
        target.damage(dmg, hero, DamageType.COLD);

        if (target.isAlive()) {
            ArtsAttachment.apply(target, ArtsAttachment.ArtsType.CRYO, hero);
        }
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (강력한 일격까지 영구, 소모 후 detach)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 저온 주입 아이콘
    }
}
