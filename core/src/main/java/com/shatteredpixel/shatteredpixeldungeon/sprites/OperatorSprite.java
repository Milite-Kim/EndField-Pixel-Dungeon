/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonWallsTilemap;
import com.watabou.noosa.TextureFilm;

/**
 * 오퍼레이터 인게임 스프라이트.
 *
 * SPD 엔진 계층인 {@link HeroSprite}를 확장하여 sprint()/fly/avatar 등 기존 기능을
 * 그대로 상속하면서, 텍스처·프레임 셋업(updateArmor)만 64×64 오퍼레이터 시트로 교체한다.
 *
 * {@code Hero.moveSpeed()}가 sprite를 {@code (HeroSprite)}로 무조건 캐스팅하므로,
 * 메인 오퍼레이터 sprite는 반드시 HeroSprite의 하위 타입이어야 한다.
 *
 * 현재는 스탠딩 1프레임만 존재 → 모든 애니메이션이 프레임 0을 공유한다.
 * 모션 시트 제작 후 각 애니메이션의 프레임 인덱스만 갱신하면 된다.
 *
 * 자세한 설계: docs/엔픽던_스프라이트시스템.md
 */
public class OperatorSprite extends HeroSprite {

	private static final int FRAME_SIZE     = 64;
	private static final int RUN_FRAMERATE  = 20;

	public OperatorSprite() {
		super();
	}

	@Override
	public void updateArmor() {
		Operator op = (Dungeon.hero != null) ? Dungeon.hero.activeMainOperator : null;
		String sheet = (op != null) ? op.spriteSheet() : null;

		// 스프라이트 미제작 오퍼레이터 → 기존 HeroSprite 셋업으로 폴백
		if (sheet == null) {
			super.updateArmor();
			return;
		}

		texture( sheet );
		TextureFilm film = new TextureFilm( texture, FRAME_SIZE, FRAME_SIZE );

		// 스탠딩 1프레임 — 모든 애니메이션이 프레임 0 고정.
		// 모션 추가 시 각 애니메이션의 frames(...) 인덱스만 교체.
		idle = new Animation( 1, true );
		idle.frames( film, 0 );

		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 0 );

		die = new Animation( 8, false );
		die.frames( film, 0 );

		// attack/operate는 마지막 프레임에서 onComplete() 콜백이 발동하므로
		// 반드시 2프레임 이상으로 둬야 공격 후 콜백 체인이 정상 동작한다.
		attack = new Animation( 15, false );
		attack.frames( film, 0, 0 );

		zap = attack.clone();

		operate = new Animation( 8, false );
		operate.frames( film, 0, 0 );

		if (Dungeon.hero != null && Dungeon.hero.isAlive()) {
			idle();
		} else {
			die();
		}
	}

	// ─────────────────────────────────────────────
	// 벽 오버행 가림 회피 (skipCells)
	// ─────────────────────────────────────────────
	//
	// DungeonWallsTilemap은 '남쪽 칸이 벽/문이면 그 칸에 오버행을 그리는' 방식이며,
	// walls 레이어가 mobs 레이어보다 나중에 그려진다.
	//   · 캐릭터 남쪽의 벽 → 캐릭터 칸에 오버행 → 하단부가 가려짐 (의도된 깊이 표현, 유지해야 함)
	//   · 캐릭터 북쪽의 벽 → 16px 스프라이트는 침범하지 않아 원래 문제 없음
	//
	// 그러나 오퍼레이터 스프라이트는 64×64라 머리가 북쪽 칸까지 뻗어,
	// '뒤에 있어야 할' 북쪽 벽에 머리가 잘린다.
	//
	// 레이어 순서를 뒤집으면 남쪽 벽의 하단 가림까지 잃으므로,
	// SPD가 자체 대형 스프라이트(CrystalSpireSprite / FungalCoreSprite)에 쓰는 방식 그대로
	// **머리가 침범하는 북쪽 칸의 벽 렌더링만 선택적으로 생략**한다.
	// → 북쪽 벽에 안 잘리면서 남쪽 벽의 하단 가림은 그대로 유지된다.

	/** 머리가 침범하는 북쪽 칸 수. 실제 캐릭터 몸체(약 32px = 2타일) 기준. */
	private static final int WALL_SKIP_ROWS = 2;

	/** 현재 skipCells를 적용해 둔 기준 위치 (-1 = 미적용) */
	private int wallSkipAnchor = -1;

	@Override
	public void update() {
		super.update();
		updateWallSkip();
	}

	/** 위치·가시성 변화에 맞춰 북쪽 칸 벽 생략을 갱신한다. */
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
