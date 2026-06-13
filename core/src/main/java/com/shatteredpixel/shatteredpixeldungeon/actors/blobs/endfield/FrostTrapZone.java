/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs.endfield;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frozen;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;

/**
 * 냉기 장치 지대 (FrostTrapZone) — 이본 '꽁꽁이' 배틀스킬.
 *
 * 매 턴 냉기 충격파(틱 피해), 종료 시 폭발 → 냉기 피해 + 강제 동결.
 * 폭발 동결을 쓰려면 explosionDamage > 0 으로 소환.
 */
public class FrostTrapZone extends EndfieldZone {

    /** 종료 폭발 시 강제 동결 스택 수(지속 결정). TODO: 수치 확정 */
    public static final int FORCED_FREEZE_STACKS = 3;

    @Override
    protected DamageType damageType() {
        return DamageType.COLD;
    }

    /** 종료 폭발: 강제 동결. */
    @Override
    protected void onExplode(Char ch) {
        Frozen.apply(ch, FORCED_FREEZE_STACKS);
    }

    @Override
    public void use(BlobEmitter emitter) {
        super.use(emitter);
        emitter.start(SnowParticle.FACTORY, 0.05f, 0);
    }
}
