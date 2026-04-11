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
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

/**
 * 연계기 발동 시 우상단에 표시되는 오퍼레이터 얼굴 팝업.
 *
 * 연계기 발동 오퍼레이터의 얼굴 이미지를 작게 팝업으로 보여준다.
 * 에셋 미확정 시 속성 색상 placeholder 표시.
 *
 * 애니메이션 (TODO: 수치 확정):
 *   팝업 진입  POP_IN_TIME   = 0.12s — 우에서 슬라이드 인 + 알파 페이드 인
 *   유지       HOLD_TIME     = 1.50s
 *   페이드 아웃 FADE_OUT_TIME = 0.25s
 *
 * 위치: uiCamera 우상단 (MARGIN 여백).
 * 크기: FACE_SIZE × FACE_SIZE (+ BORDER 테두리).
 *
 * 동시 발동 처리: 기존 팝업이 살아있으면 즉시 대체한다.
 */
public class ChainFacePopup extends Group {

    // ── 크기/위치 상수 ────────────────────────────
    private static final float FACE_SIZE  = 32f;  // 얼굴 이미지 표시 크기 (정사각형)
    private static final float BORDER     = 2f;   // 테두리 두께
    private static final float MARGIN     = 4f;   // 화면 가장자리 여백

    // ── 타이밍 상수 (TODO: 수치 확정) ────────────────
    private static final float POP_IN_TIME   = 0.12f;
    private static final float HOLD_TIME     = 1.50f;
    private static final float FADE_OUT_TIME = 0.25f;

    // ── 속성별 placeholder 색상 ──────────────────
    private static final int[] ATTR_COLORS = {
        0xFF888888, // PHYSICAL
        0xFFCC4400, // HEAT
        0xFF4488CC, // COLD
        0xFF44AA44, // NATURE
        0xFFCCCC00, // ELECTRIC
    };

    // ── 상태 머신 ─────────────────────────────────
    private enum Phase { POP_IN, HOLD, FADE_OUT }
    private Phase phase = Phase.POP_IN;
    private float elapsed = 0f;

    /** 팝업 슬라이드 시작 X (화면 밖 오른쪽) */
    private float startX;
    /** 팝업 최종 안착 X */
    private float targetX;
    /** 팝업 Y (top) */
    private float popY;

    /** 현재 그룹 위치 (Group은 x/y 필드가 없으므로 직접 추적) */
    private float groupX = 0f, groupY = 0f;

    // ── 현재 살아있는 팝업 (대체 처리용) ─────────────
    private static ChainFacePopup current;

    // ─────────────────────────────────────────────

    private ChainFacePopup(Operator operator) {
        super();
        camera = PixelScene.uiCamera;

        float W = camera.width;
        float totalSize = FACE_SIZE + BORDER * 2;

        targetX = W - totalSize - MARGIN;
        startX  = W;               // 오른쪽 화면 밖에서 시작
        popY    = MARGIN;

        // ── 테두리 (배경) ──────────────────────────
        int attrIdx = operator.attribute().ordinal();
        int borderColor = ATTR_COLORS[Math.min(attrIdx, ATTR_COLORS.length - 1)];
        ColorBlock border = new ColorBlock(totalSize, totalSize, borderColor);
        border.alpha(0f);
        add(border);

        // ── 얼굴 이미지 또는 placeholder ──────────
        String faceAsset = operator.chainFaceAsset();
        if (faceAsset != null) {
            Image face = new Image(faceAsset);
            // fill+center 스케일
            float scale = FACE_SIZE / Math.max(face.width, face.height);
            face.scale.set(scale);
            face.x = BORDER + (FACE_SIZE - face.width  * scale) / 2f;
            face.y = BORDER + (FACE_SIZE - face.height * scale) / 2f;
            face.alpha(0f);
            add(face);
        } else {
            // placeholder: 어두운 배경 + 속성 색상 내부 블록
            ColorBlock inner = new ColorBlock(FACE_SIZE, FACE_SIZE, 0xFF111111);
            inner.x = BORDER;
            inner.y = BORDER;
            inner.alpha(0f);
            add(inner);

            // 속성 색상 포인트 (중앙 소형 블록)
            float dot = 8f;
            ColorBlock dot_ = new ColorBlock(dot, dot, borderColor);
            dot_.x = BORDER + (FACE_SIZE - dot) / 2f;
            dot_.y = BORDER + (FACE_SIZE - dot) / 2f;
            dot_.alpha(0f);
            add(dot_);
        }

        // 초기 위치 — 오른쪽 밖 (Group은 x/y 없으므로 자식들 좌표를 직접 이동)
        setGroupPos(startX, popY);
    }

    /**
     * 연계기 발동 오퍼레이터의 얼굴 팝업을 표시한다.
     * 기존 팝업이 있으면 즉시 대체한다.
     *
     * @param operator   연계기를 발동한 오퍼레이터
     * @param scene      팝업을 추가할 씬 그룹 (GameScene에서 전달)
     */
    public static void show(Operator operator, Group scene) {
        if (current != null && current.alive) {
            current.killAndErase();
        }
        ChainFacePopup popup = new ChainFacePopup(operator);
        scene.addToFront(popup);
        current = popup;
    }

    // ─────────────────────────────────────────────
    // update — 상태 머신
    // ─────────────────────────────────────────────

    @Override
    public void update() {
        super.update();

        elapsed += Game.elapsed;

        switch (phase) {
            case POP_IN: {
                float progress = Math.min(elapsed / POP_IN_TIME, 1f);
                // ease-out cubic
                float ease = 1f - (1f - progress) * (1f - progress) * (1f - progress);
                setGroupPos(startX + (targetX - startX) * ease, groupY);
                setGroupAlpha(ease);
                if (elapsed >= POP_IN_TIME) {
                    setGroupPos(targetX, groupY);
                    setGroupAlpha(1f);
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
                setGroupAlpha(1f - progress);
                if (elapsed >= FADE_OUT_TIME) {
                    if (current == this) current = null;
                    killAndErase();
                }
                break;
            }
        }
    }

    /** Group 전체 알파 설정 (alpha()는 개별 Visual에만 적용되므로 멤버 순회) */
    private void setGroupAlpha(float a) {
        for (int i = 0; i < length; i++) {
            if (members.get(i) instanceof com.watabou.noosa.Visual) {
                ((com.watabou.noosa.Visual) members.get(i)).alpha(a);
            }
        }
    }

    /** Group 전체 위치 이동 (Group은 x/y 필드가 없으므로 자식 Visual 좌표를 직접 옮긴다) */
    private void setGroupPos(float gx, float gy) {
        float dx = gx - groupX, dy = gy - groupY;
        groupX = gx; groupY = gy;
        for (int i = 0; i < length; i++) {
            if (members.get(i) instanceof com.watabou.noosa.Visual) {
                com.watabou.noosa.Visual v = (com.watabou.noosa.Visual) members.get(i);
                v.x += dx;
                v.y += dy;
            }
        }
    }
}
