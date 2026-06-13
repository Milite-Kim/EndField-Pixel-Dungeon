/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs.endfield;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtsAttachment;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;

/**
 * 자연 지대 (NatureZone) — 매 턴 자연 피해 + 감속, 종료 시 폭발(자연 부착).
 *
 * 질베르타 '중력 특이점' 등에서 사용.
 * onTick: 감속(Cripple) 부여 / onExplode: 자연 부착(ArtsAttachment.NATURE) 부여.
 * 폭발 효과를 쓰려면 explosionDamage > 0 으로 소환.
 */
public class NatureZone extends EndfieldZone {

    @Override
    protected DamageType damageType() {
        return DamageType.NATURE;
    }

    /** 매 턴: 지대 위 적에게 감속 부여. */
    @Override
    protected void onTick(Char ch) {
        Buff.affect(ch, Cripple.class, Cripple.DURATION);
    }

    /** 종료 폭발: 자연 부착 부여 (소멸하는 중력장의 마지막 반응). */
    @Override
    protected void onExplode(Char ch) {
        if (Dungeon.hero != null) {
            ArtsAttachment.apply(ch, ArtsAttachment.ArtsType.NATURE, Dungeon.hero);
        }
    }

    @Override
    public void use(BlobEmitter emitter) {
        super.use(emitter);
        emitter.start(LeafParticle.LEVEL_SPECIFIC, 0.1f, 0);
    }
}
