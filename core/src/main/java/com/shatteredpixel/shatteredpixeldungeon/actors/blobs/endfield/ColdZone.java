/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs.endfield;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;

/**
 * 냉기 지대 (ColdZone) — 매 턴 냉기 피해를 주는 장판.
 *
 * 스노우샤인 궁극기 등에서 사용. tickDamage/explosionDamage는 시전 시 주입.
 * 폭발이 필요한 경우 explosionDamage > 0 으로 소환하면 종료 시 폭발한다.
 */
public class ColdZone extends EndfieldZone {

    @Override
    protected DamageType damageType() {
        return DamageType.COLD;
    }

    @Override
    public void onTick(Char ch) {
        // 부가 효과 없음 (순수 냉기 피해 지대). 필요 시 감속/부착 등 추가 가능.
    }

    @Override
    public void use(BlobEmitter emitter) {
        super.use(emitter);
        emitter.start(SnowParticle.FACTORY, 0.05f, 0);
    }
}
