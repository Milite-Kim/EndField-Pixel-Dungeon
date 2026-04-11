/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;

/**
 * 궁극기 컷씬 오버레이.
 *
 * 오퍼레이터마다 고유 컷씬 이미지(메인 일러스트와 별도)를 전체화면으로 표시한다.
 * 텍스트 없음. 페이드 인 → 유지 → 페이드 아웃 후 콜백으로 실제 궁극기 발동.
 *
 * 사용법:
 *   UltimateCutscene.show(operator, () -> { /* 궁극기 실제 실행 *\/ });
 *
 * 에셋 미확정(operator.cutsceneAsset() == null)이면 컷씬 없이 콜백 즉시 호출.
 *
 * 타이밍 (TODO: 수치 확정):
 *   페이드 인  FADE_IN_TIME  = 0.25s
 *   유지       HOLD_TIME     = 1.20s
 *   페이드 아웃 FADE_OUT_TIME = 0.35s
 */
public class UltimateCutscene extends Group {

    // ── 타이밍 상수 (TODO: 수치 확정) ────────────────
    private static final float FADE_IN_TIME  = 0.25f;
    private static final float HOLD_TIME     = 1.20f;
    private static final float FADE_OUT_TIME = 0.35f;

    // ── 상태 머신 ─────────────────────────────────
    private enum Phase { FADE_IN, HOLD, FADE_OUT }
    private Phase phase = Phase.FADE_IN;
    private float elapsed = 0f;

    // ── UI 요소 ───────────────────────────────────
    /** 전체화면 검정 배경 */
    private final ColorBlock backdrop;
    /** 컷씬 이미지 (에셋 있을 때만 non-null) */
    private final Image art;
    /** 입력 블로커 — 컷씬 중 게임 조작 차단 */
    private final PointerArea blocker;

    /** 페이드 아웃 완료 후 호출할 콜백 (실제 궁극기 실행) */
    private final Runnable onComplete;

    // ─────────────────────────────────────────────

    private UltimateCutscene(String assetPath, Runnable onComplete) {
        super();
        this.onComplete = onComplete;

        camera = PixelScene.uiCamera;

        float W = camera.width;
        float H = camera.height;

        // 전체화면 검정 배경
        backdrop = new ColorBlock(W, H, 0xFF000000);
        backdrop.alpha(0f);
        add(backdrop);

        // 컷씬 이미지 — fill+center 방식으로 스케일
        if (assetPath != null) {
            art = new Image(assetPath);
            float scaleX = W / art.width;
            float scaleY = H / art.height;
            float fillScale = Math.max(scaleX, scaleY);
            art.scale.set(fillScale);
            art.x = (W - art.width  * fillScale) / 2f;
            art.y = (H - art.height * fillScale) / 2f;
            art.alpha(0f);
            add(art);
        } else {
            art = null;
        }

        // 입력 블로커 — 전체 화면 영역 터치 무시
        blocker = new PointerArea(0, 0, W, H) {
            @Override
            protected void onClick(PointerEvent event) { /* 컷씬 중 클릭 무시 */ }
        };
        blocker.camera = camera;
        add(blocker);
    }

    /**
     * 궁극기 컷씬을 표시한다.
     *
     * 에셋이 없으면 컷씬을 건너뛰고 onComplete를 즉시 호출한다.
     *
     * @param operator   궁극기를 사용하는 오퍼레이터
     * @param onComplete 컷씬 종료 후 실행할 콜백 (실제 궁극기 실행)
     */
    public static void show(Operator operator, Runnable onComplete) {
        String assetPath = operator.cutsceneAsset();

        if (assetPath == null) {
            // 에셋 미확정 — 컷씬 없이 즉시 발동
            onComplete.run();
            return;
        }

        UltimateCutscene cutscene = new UltimateCutscene(assetPath, onComplete);
        GameScene.addToFront(cutscene);
    }

    // ─────────────────────────────────────────────
    // update — 상태 머신
    // ─────────────────────────────────────────────

    @Override
    public void update() {
        super.update();

        elapsed += Game.elapsed;

        switch (phase) {
            case FADE_IN: {
                float progress = Math.min(elapsed / FADE_IN_TIME, 1f);
                setAlpha(progress);
                if (elapsed >= FADE_IN_TIME) {
                    setAlpha(1f);
                    phase = Phase.HOLD;
                    elapsed = 0f;
                }
                break;
            }
            case HOLD: {
                if (elapsed >= HOLD_TIME) {
                    phase = Phase.FADE_OUT;
                    elapsed = 0f;
                }
                break;
            }
            case FADE_OUT: {
                float progress = Math.min(elapsed / FADE_OUT_TIME, 1f);
                setAlpha(1f - progress);
                if (elapsed >= FADE_OUT_TIME) {
                    setAlpha(0f);
                    finish();
                }
                break;
            }
        }
    }

    /** backdrop + art 동시에 투명도 조정 */
    private void setAlpha(float a) {
        backdrop.alpha(a);
        if (art != null) art.alpha(a);
    }

    /** 컷씬 종료 처리 */
    private void finish() {
        killAndErase(); // 씬에서 자신을 제거
        onComplete.run();
    }
}
