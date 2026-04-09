/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;

public class HeroAction {
	
	public int dst;
	
	public static class Move extends HeroAction {
		public Move( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class PickUp extends HeroAction {
		public PickUp( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class OpenChest extends HeroAction {
		public OpenChest( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class Buy extends HeroAction {
		public Buy( int dst ) {
			this.dst = dst;
		}
	}
	
	public static class Interact extends HeroAction {
		public Char ch;
		public Interact( Char ch ) {
			this.ch = ch;
		}
	}
	
	public static class Unlock extends HeroAction {
		public Unlock( int door ) {
			this.dst = door;
		}
	}
	
	public static class LvlTransition extends HeroAction {
		public LvlTransition(int stairs ) {
			this.dst = stairs;
		}
	}

	public static class Mine extends HeroAction {
		public Mine( int wall ) {
			this.dst = wall;
		}
	}
	
	public static class Alchemy extends HeroAction {
		public Alchemy( int pot ) {
			this.dst = pot;
		}
	}
	
	public static class Attack extends HeroAction {
		public Char target;
		public Attack( Char target ) {
			this.target = target;
		}
	}

	/**
	 * 배틀스킬 발동 액션.
	 *
	 * dst = 타겟 셀 위치.
	 * 해당 셀에 Char가 있으면 Char 타겟팅, 없으면 지면 타겟팅(canTargetCell 스킬).
	 * 사거리 초과 시 autoApproach() 설정에 따라 접근 후 발동 or 메시지 후 취소.
	 */
	public static class UseBattleSkill extends HeroAction {
		public UseBattleSkill( int cell ) {
			this.dst = cell;
		}
	}

	/**
	 * 궁극기 발동 액션.
	 * 배틀스킬과 동일한 타겟팅 방식.
	 * dst = 타겟 셀 위치 (selfTarget 궁극기는 hero.pos, 지면형은 빈 셀 가능).
	 */
	public static class UseUltimate extends HeroAction {
		public UseUltimate( int cell ) {
			this.dst = cell;
		}
	}

	/**
	 * 아츠유닛 충전 활성화 액션.
	 * 타겟팅 없이 즉시 발동. 1턴 소모.
	 * Operator.activateArtsCharge(hero) 호출 후 충전 소모.
	 */
	public static class UseArtsCharge extends HeroAction { }
}
