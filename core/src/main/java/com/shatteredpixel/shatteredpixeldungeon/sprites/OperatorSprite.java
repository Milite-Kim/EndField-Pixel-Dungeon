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
}
