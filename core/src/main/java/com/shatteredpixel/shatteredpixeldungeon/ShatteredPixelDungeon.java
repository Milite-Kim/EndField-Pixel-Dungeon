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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.WelcomeScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PlatformSupport;

public class ShatteredPixelDungeon extends Game {

	//rankings from v1.2.3 and older use a different score formula, so this reference is kept
	public static final int v1_2_3 = 628;

	//savegames from versions older than v2.5.4 are no longer supported, and data from them is ignored
	public static final int v2_5_4 = 802;

	public static final int v3_0_2 = 833;
	public static final int v3_1_1 = 850;
	public static final int v3_2_5 = 877;
	public static final int v3_3_0 = 883;
	
	public ShatteredPixelDungeon( PlatformSupport platform ) {
		super( sceneClass == null ? WelcomeScene.class : sceneClass, platform );

		//pre-v3.3.0
		com.watabou.utils.Bundle.addAlias(
				com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey.class,
				"com.shatteredpixel.shatteredpixeldungeon.items.keys.SkeletonKey" );

		// 엔픽던: 오퍼레이터 클래스명을 공식 영문 표기로 변경 (구 세이브 호환용 별칭)
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Perlica.class,    "Felika");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.ChenQianyu.class, "Jincheonwoo");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Wulfgard.class,   "Wolfguard");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Xaihi.class,      "Zaihe");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Catcher.class,    "Kachir");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.DaPan.class,      "Pan");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Lifeng.class,     "Yeofung");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Rossi.class,      "Rosi");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Laevatain.class,  "Levatine");
		addOperatorAlias(com.shatteredpixel.shatteredpixeldungeon.operators.team.Avywenna.class,   "Aviena");

		// 엔픽던: 보스 클래스명을 공식 영문 표기로 변경 (구 세이브 호환용 별칭)
		com.watabou.utils.Bundle.addAlias(
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.endfield.Rhodagn.class,
				"com.shatteredpixel.shatteredpixeldungeon.actors.mobs.endfield.Rodan" );
	}

	/** 구 오퍼레이터 클래스명(단순명) → 현재 클래스 별칭 등록 */
	private static void addOperatorAlias( Class<?> current, String oldSimpleName ) {
		com.watabou.utils.Bundle.addAlias( current,
				"com.shatteredpixel.shatteredpixeldungeon.operators.team." + oldSimpleName );
	}
	
	@Override
	public void create() {
		super.create();

		updateSystemUI();
		SPDAction.loadBindings();
		
		Music.INSTANCE.enable( SPDSettings.music() );
		Music.INSTANCE.volume( SPDSettings.musicVol()*SPDSettings.musicVol()/100f );
		Sample.INSTANCE.enable( SPDSettings.soundFx() );
		Sample.INSTANCE.volume( SPDSettings.SFXVol()*SPDSettings.SFXVol()/100f );

		Sample.INSTANCE.load( Assets.Sounds.all );
		
	}

	@Override
	public void finish() {
		if (!DeviceCompat.isiOS()) {
			super.finish();
		} else {
			//can't exit on iOS (Apple guidelines), so just go to title screen
			switchScene(TitleScene.class);
		}
	}

	public static void switchNoFade(Class<? extends PixelScene> c){
		switchNoFade(c, null);
	}

	public static void switchNoFade(Class<? extends PixelScene> c, SceneChangeCallback callback) {
		PixelScene.noFade = true;
		switchScene( c, callback );
	}
	
	public static void seamlessResetScene(SceneChangeCallback callback) {
		if (scene() instanceof PixelScene){
			((PixelScene) scene()).saveWindows();
			switchNoFade((Class<? extends PixelScene>) sceneClass, callback );
		} else {
			resetScene();
		}
	}
	
	public static void seamlessResetScene(){
		seamlessResetScene(null);
	}
	
	@Override
	protected void switchScene() {
		super.switchScene();
		if (scene instanceof PixelScene){
			((PixelScene) scene).restoreWindows();
		}
	}
	
	@Override
	public void resize( int width, int height ) {
		if (width == 0 || height == 0){
			return;
		}

		if (scene instanceof PixelScene &&
				(height != Game.height || width != Game.width)) {
			PixelScene.noFade = true;
			((PixelScene) scene).saveWindows();
		}

		super.resize( width, height );

		updateDisplaySize();

	}
	
	@Override
	public void destroy(){
		super.destroy();
		GameScene.endActorThread();
	}
	
	public void updateDisplaySize(){
		platform.updateDisplaySize();
	}

	public static void updateSystemUI() {
		platform.updateSystemUI();
	}
}