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
 * 스노우샤인 패리 (SnowshineParry) — 스노우샤인 배틀스킬 버프
 *
 * 스노우샤인 배틀스킬 사용 시 Hero에게 부여.
 * 다음 물리 공격 1회를 완전 차단 → 반격: 냉기 피해 + 냉기 부착.
 *
 * KachirParry와 동일한 구조:
 *   Hero.damage(int, Object) 에서 src instanceof Char && 물리 피해 시 interceptPhysical() 호출.
 *
 * TODO: 아츠 피해 70% 감소 — DamageType-aware 훅 완성 후 추가
 */
public class SnowshineParry extends Buff {

    { type = buffType.POSITIVE; announced = true; }

    private float counterMult = 0.7f;

    public SnowshineParry setCounterMult(float mult) {
        this.counterMult = mult;
        return this;
    }

    // ─────────────────────────────────────────────
    // 피격 차단 & 반격
    // ─────────────────────────────────────────────

    /**
     * Hero.damage(int, Object)에서 물리 피격 시 호출.
     * 피해를 완전 차단하고 반격(냉기 피해 + 냉기 부착) 후 버프 소모.
     *
     * @return 실제로 받아야 할 피해 (0 = 완전 차단)
     */
    public int interceptPhysical(Hero hero, Char attacker, int dmg) {
        // 반격: 냉기 피해
        int counterDmg = Math.round(hero.damageRoll() * counterMult);
        attacker.damage(counterDmg, hero, DamageType.COLD);

        // 냉기 부착
        if (attacker.isAlive()) {
            ArtsAttachment.apply(attacker, ArtsAttachment.ArtsType.CRYO, hero);
        }

        detach();
        return 0; // 피해 완전 차단
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (1회 소모형)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 스노우샤인 패리 아이콘
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String COUNTER_MULT = "counterMult";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COUNTER_MULT, counterMult);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        counterMult = bundle.getFloat(COUNTER_MULT);
    }
}
