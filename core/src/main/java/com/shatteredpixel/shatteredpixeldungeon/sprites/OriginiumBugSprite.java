/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

/**
 * 원석충 (OriginiumSlug) 스프라이트.
 *
 * 시트: sprites/OriginiumBug.png — 440×16, 22×16 프레임 20장.
 *   0        : 공격
 *   1~7      : 대기 / 이동
 *   8~12     : 사망
 */
public class OriginiumBugSprite extends MobSprite {

    public OriginiumBugSprite() {
        super();

        texture( Assets.Sprites.ORIGINIUM_BUG );

        TextureFilm frames = new TextureFilm( texture, 22, 16 );

        idle = new Animation( 8, true );
        idle.frames( frames, 1, 2, 3, 4, 5, 6, 7 );

        run = new Animation( 10, true );
        run.frames( frames, 1, 2, 3, 4, 5, 6, 7 );

        attack = new Animation( 15, false );
        attack.frames( frames, 0, 0 );

        die = new Animation( 8, false );
        die.frames( frames, 8, 9, 10, 11, 12 );

        play( idle );
    }
}
