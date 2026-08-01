/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonWallsTilemap;
import com.watabou.noosa.TextureFilm;

/**
 * 로댄 (Rhodagn) — A5 보스 스프라이트.
 *
 * 시트: sprites/Rhodagn.png — 48×48 단일 프레임.
 * 타일(16×16)의 3배 크기로, SPD 최대 보스(DM300 25×22)보다 크다.
 *
 * 모션 시트 제작 전까지 모든 애니메이션이 프레임 0을 공유한다.
 * attack/die는 마지막 프레임에서 onComplete() 콜백이 발동하므로 2프레임 이상으로 둔다.
 *
 * 큰 스프라이트라 머리가 북쪽 칸까지 뻗어 벽 오버행에 잘리므로,
 * OperatorSprite와 동일하게 skipCells로 북쪽 칸 벽 렌더링을 생략한다.
 * (자세한 배경: docs/엔픽던_스프라이트시스템.md 2-B절)
 */
public class RhodagnSprite extends MobSprite {

    private static final int FRAME_SIZE = 48;

    /** 머리가 침범하는 북쪽 칸 수 (48px ≈ 3타일) */
    private static final int WALL_SKIP_ROWS = 2;

    public RhodagnSprite() {
        super();

        texture( Assets.Sprites.RHODAGN );

        TextureFilm frames = new TextureFilm( texture, FRAME_SIZE, FRAME_SIZE );

        idle = new Animation( 2, true );
        idle.frames( frames, 0 );

        run = new Animation( 8, true );
        run.frames( frames, 0 );

        attack = new Animation( 10, false );
        attack.frames( frames, 0, 0 );

        zap = attack.clone();

        die = new Animation( 8, false );
        die.frames( frames, 0, 0 );

        play( idle );
    }

    // ─────────────────────────────────────────────
    // 벽 오버행 가림 회피 (skipCells)
    // ─────────────────────────────────────────────

    /** 현재 skipCells를 적용해 둔 기준 위치 (-1 = 미적용) */
    private int wallSkipAnchor = -1;

    @Override
    public void update() {
        super.update();
        updateWallSkip();
    }

    private void updateWallSkip() {
        int desired = (ch != null && visible && curAnim != die) ? ch.pos : -1;
        if (desired == wallSkipAnchor) return;

        clearWallSkip();
        if (desired >= 0) applyWallSkip(desired);
        wallSkipAnchor = desired;
    }

    private void applyWallSkip(int pos) {
        if (Dungeon.level == null) return;
        int w = Dungeon.level.width();
        for (int i = 1; i <= WALL_SKIP_ROWS; i++) {
            int cell = pos - i * w;
            if (cell < 0) break;
            DungeonWallsTilemap.skipCells.add(cell);
            GameScene.updateMap(cell);
        }
    }

    private void clearWallSkip() {
        if (wallSkipAnchor < 0 || Dungeon.level == null) return;
        int w = Dungeon.level.width();
        for (int i = 1; i <= WALL_SKIP_ROWS; i++) {
            int cell = wallSkipAnchor - i * w;
            if (cell < 0) break;
            DungeonWallsTilemap.skipCells.remove(cell);
            GameScene.updateMap(cell);
        }
    }

    @Override
    public void die() {
        clearWallSkip();
        wallSkipAnchor = -1;
        super.die();
    }

    @Override
    public void destroy() {
        clearWallSkip();
        wallSkipAnchor = -1;
        super.destroy();
    }
}
