/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.operators.OperatorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.operators.team.Endministrator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOperatorInfo;
import com.watabou.glwrap.Texture;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.utils.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * 메인 오퍼레이터 선택 화면.
 *
 * 레이아웃:
 *  - 일러스트: 전체 너비 × (전체 높이 - 이름 바 - 초상화 행)
 *  - 속성 탭: 일러스트 상단에 오버레이 (반투명 배경)
 *  - 이름 바: 일러스트 바로 아래 — 속성 색상 마커 + 이름 + 진행 버튼
 *  - 초상화 행: 화면 최하단, 너비 중앙 정렬
 *  - 뒤로가기 버튼: 좌상단 (일러스트 위 오버레이)
 *
 * 일러스트 표시 방식:
 *  fill+center crop — Camera 기반 glScissor 클리핑으로 영역 밖 overflow 차단.
 */
public class OperatorSelectScene extends PixelScene {

    // 한 페이지에 보여줄 초상화 수
    private static final int PORTRAITS_PER_PAGE = 7;

    // 초상화 크기/간격
    private static final int PORTRAIT_SIZE   = 28;
    private static final int PORTRAIT_GAP    = 2;

    // 탭 크기 (일러스트 위 오버레이)
    private static final int TAB_HEIGHT = 14;

    // 이름 바 높이
    private static final float NAME_BAR_H = 24f;

    // 일러스트 하단 그라데이션 (밴드 수 / 최종 알파)
    private static final int   FADE_STEPS     = 12;
    private static final float FADE_MAX_ALPHA = 0.85f;

    // 이름 바 좌우 버튼 크기 (좌: 정보 / 우: 진행) — 대칭이라 이름이 화면 정중앙에 온다
    private static final float SIDE_BTN_W = 50f;

    // 속성 탭 색상
    private static final int[] ATTR_COLORS = {
        0xFF888888, // PHYSICAL  물리
        0xFFCC4400, // HEAT      열기
        0xFF4488CC, // COLD      냉기
        0xFF44AA44, // NATURE    자연
        0xFFCCCC00, // ELECTRIC  전기
    };

    private static final Operator.Attribute[] ATTRS = Operator.Attribute.values();

    // ─── 상태 ───────────────────────────────────
    private Operator.Attribute selectedAttr = Operator.Attribute.PHYSICAL;
    private Class<? extends Operator> selectedOpClass = Endministrator.class;
    private int currentPage = 0;

    private List<Class<? extends Operator>> currentAttrOps = new ArrayList<>();

    // ─── 일러스트 영역 ───────────────────────────
    private float illusX, illusY, illusW, illusH;

    /** glScissor 클리핑용 전용 카메라 */
    private Camera illusCamera;

    /**
     * 일러스트 전용 Group.
     * camera = illusCamera → 내부 Image는 이 카메라 좌표계에서 렌더링되며
     * 카메라 바운드 밖의 픽셀은 자동으로 클리핑됨.
     */
    private Group illusGroup;

    /** 현재 표시 중인 일러스트 Image (없으면 null) */
    private Image currentIllus = null;

    // ─── UI 요소 ─────────────────────────────────
    private ColorBlock illustrationBg;
    private RenderedTextBlock illustrationLabel;  // 일러스트 없을 때 placeholder

    private ColorBlock[] tabBgs   = new ColorBlock[ATTRS.length];
    private ColorBlock[] tabLines = new ColorBlock[ATTRS.length];
    private RenderedTextBlock[] tabLabels = new RenderedTextBlock[ATTRS.length];

    // 이름 바 요소
    private ColorBlock   nameBarAttrMarker; // 속성 색상 마커 (좌측)
    private RenderedTextBlock nameLabel;    // 오퍼레이터 이름

    private PortraitBtn[] portraitBtns = new PortraitBtn[PORTRAITS_PER_PAGE];

    private IconButton btnPrev;
    private IconButton btnNext;
    private StyledButton btnProceed;
    private StyledButton btnInfo;
    private IconButton btnBack;

    @Override
    public void create() {
        super.create();

        OperatorRegistry.loadGlobal();

        float W = Camera.main.width;
        float H = Camera.main.height;

        // ── 배경 (전체 화면) ──────────────────────
        ColorBlock sceneBg = new ColorBlock(W, H, 0xFF111820);
        add(sceneBg);

        // ── 하단 UI 높이 계산 ───────────────────────
        // 이름 바 + 초상화 행 + 여백
        float portraitRowH = PORTRAIT_SIZE + 4f;
        float bottomH = NAME_BAR_H + portraitRowH + 4f;

        // ── 일러스트 영역: 전체 너비 × (탭 아래 ~ 하단 UI 위) ──
        // 상단 속성 탭과 겹치지 않도록 탭 영역(TAB_HEIGHT+2) 아래에서 시작한다.
        float tabAreaH = TAB_HEIGHT + 2f;
        illusX = 0f;
        illusY = tabAreaH;
        illusW = W;
        illusH = H - bottomH - illusY;

        illustrationBg = new ColorBlock(illusW, illusH, 0xFF1a1a2a);
        illustrationBg.x = illusX;
        illustrationBg.y = illusY;
        add(illustrationBg);

        // ── 일러스트 카메라 + Group ──────────────────
        Point camP = Camera.main.cameraToScreen(illusX, illusY);
        illusCamera = new Camera(camP.x, camP.y, (int)illusW, (int)illusH, defaultZoom);
        Camera.add(illusCamera);

        illusGroup = new Group();
        illusGroup.camera = illusCamera;
        add(illusGroup);

        // ── placeholder 텍스트 ─────────────────────
        illustrationLabel = renderTextBlock("", 9);
        illustrationLabel.hardlight(0xCCFFCC);
        add(illustrationLabel);

        // ── 일러스트 하단 그라데이션 (이름 바로 자연스럽게 이어지도록) ──
        // ColorBlock은 단색이라 한 장만 깔면 경계가 뚜렷한 '검은 띠'가 된다.
        // 얇은 밴드를 여러 장 쌓아 알파를 점증시켜 실제 그라데이션을 만든다.
        float fadeH  = Math.min(40f, illusH * 0.20f);
        float stepH  = fadeH / FADE_STEPS;
        for (int i = 0; i < FADE_STEPS; i++) {
            // +1f: 밴드 사이에 이음새(빈 줄)가 생기지 않도록 살짝 겹침
            ColorBlock band = new ColorBlock(W, stepH + 1f, 0xFF111820);
            band.x = 0f;
            band.y = illusY + illusH - fadeH + i * stepH;
            band.alpha((i + 1) / (float) FADE_STEPS * FADE_MAX_ALPHA);
            add(band);
        }

        // ── 속성 탭 (일러스트 위 오버레이, 상단) ─────────
        // 탭 배경 (반투명)
        ColorBlock tabAreaBg = new ColorBlock(W, TAB_HEIGHT + 2f, 0xBB111820);
        tabAreaBg.x = 0f;
        tabAreaBg.y = 0f;
        add(tabAreaBg);

        int tabCount  = ATTRS.length;
        float tabW    = (W - (tabCount - 1) * 1f) / tabCount;
        float tabStartX = 0f;
        float tabY    = 0f;

        for (int i = 0; i < tabCount; i++) {
            final int idx = i;
            final Operator.Attribute attr = ATTRS[i];

            ColorBlock bg = new ColorBlock(tabW, TAB_HEIGHT, ATTR_COLORS[i]);
            bg.x = tabStartX + i * (tabW + 1f);
            bg.y = tabY + 1f;
            bg.alpha(0.6f);
            add(bg);
            tabBgs[i] = bg;

            ColorBlock line = new ColorBlock(tabW, 2, 0xFFFFFFFF);
            line.x = bg.x;
            line.y = bg.y + TAB_HEIGHT - 2;
            line.visible = false;
            add(line);
            tabLines[i] = line;

            RenderedTextBlock label = renderTextBlock(attrLabel(attr), 6);
            label.hardlight(0xFFFFFF);
            add(label);
            tabLabels[i] = label;

            // 탭 라벨 위치
            label.setPos(bg.x + (tabW - label.width()) / 2f,
                         bg.y + (TAB_HEIGHT - label.height()) / 2f);
            align(label);

            PointerArea hit = new PointerArea(
                    bg.x, bg.y, tabW, TAB_HEIGHT) {
                @Override
                protected void onClick(PointerEvent event) {
                    selectAttr(attr);
                }
            };
            add(hit);
        }

        // ── 이름 바 (일러스트 바로 아래) ────────────────
        float nameBarY = illusY + illusH;
        ColorBlock nameBarBg = new ColorBlock(W, NAME_BAR_H, 0xEE1a2a3a);
        nameBarBg.x = 0f;
        nameBarBg.y = nameBarY;
        add(nameBarBg);

        // 속성 색상 마커 — 이름 바 좌측 가장자리 액센트 (정보 버튼과 겹치지 않게 flush)
        nameBarAttrMarker = new ColorBlock(3f, NAME_BAR_H, 0xFFFFFFFF);
        nameBarAttrMarker.x = 0f;
        nameBarAttrMarker.y = nameBarY;
        add(nameBarAttrMarker);

        // 오퍼레이터 이름 (화면 정중앙 — 좌우 버튼 폭이 같아 정확히 가운데 정렬)
        nameLabel = renderTextBlock("", 9);
        nameLabel.hardlight(Window.TITLE_COLOR);
        add(nameLabel);

        float sideBtnH = NAME_BAR_H - 6f;
        float sideBtnY = nameBarY + (NAME_BAR_H - sideBtnH) / 2f;

        // ── 정보 버튼 (이름 바 좌측) — 스킬 정보 팝업 ──
        btnInfo = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "정보") {
            @Override
            protected void onClick() {
                showOperatorInfo();
            }
        };
        btnInfo.icon(Icons.get(Icons.INFO));
        btnInfo.setSize(SIDE_BTN_W, sideBtnH);
        btnInfo.setPos(5f, sideBtnY);
        btnInfo.textColor(Window.TITLE_COLOR);
        add(btnInfo);

        // ── 진행 버튼 (이름 바 우측) ──────────────────
        btnProceed = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "진행") {
            @Override
            protected void onClick() {
                onProceed();
            }
        };
        btnProceed.icon(Icons.get(Icons.ENTER));
        btnProceed.setSize(SIDE_BTN_W, sideBtnH);
        btnProceed.setPos(W - SIDE_BTN_W - 5f, sideBtnY);
        btnProceed.textColor(Window.TITLE_COLOR);
        add(btnProceed);

        // ── 초상화 행 (화면 최하단, 중앙 정렬) ──────────
        float portraitTotalW = PORTRAITS_PER_PAGE * PORTRAIT_SIZE
                             + (PORTRAITS_PER_PAGE - 1) * PORTRAIT_GAP;
        float portraitRowX = (W - portraitTotalW) / 2f;
        float portraitRowY = nameBarY + NAME_BAR_H + 2f;

        for (int i = 0; i < PORTRAITS_PER_PAGE; i++) {
            PortraitBtn btn = new PortraitBtn();
            btn.setRect(portraitRowX + i * (PORTRAIT_SIZE + PORTRAIT_GAP),
                        portraitRowY, PORTRAIT_SIZE, PORTRAIT_SIZE);
            add(btn);
            portraitBtns[i] = btn;
        }

        // ── 이전/다음 페이지 버튼 ─────────────────────
        float arrowY = portraitRowY + (PORTRAIT_SIZE - 16) / 2f;

        btnPrev = new IconButton(Icons.get(Icons.LEFTARROW)) {
            @Override
            protected void onClick() {
                currentPage--;
                refreshPortraits();
            }
        };
        btnPrev.setRect(portraitRowX - 18f, arrowY, 16, 16);
        add(btnPrev);

        btnNext = new IconButton(Icons.get(Icons.RIGHTARROW)) {
            @Override
            protected void onClick() {
                currentPage++;
                refreshPortraits();
            }
        };
        btnNext.setRect(portraitRowX + portraitTotalW + 2f, arrowY, 16, 16);
        add(btnNext);

        // ── 뒤로가기 버튼 (속성 탭 바로 아래, 일러스트 위 오버레이) ──
        // 씬 추가 순서가 렌더 순서이므로, illusGroup보다 뒤에 추가된 이 버튼은
        // 일러스트 위에 정상적으로 그려진다. (탭과 겹치지 않도록 탭 아래로 배치)
        btnBack = new IconButton(Icons.get(Icons.ARROW)) {
            @Override
            protected void onClick() {
                onBackPressed();
            }
        };
        btnBack.setRect(4f, tabAreaH + 4f, 16, 16);
        add(btnBack);

        // ── 초기 상태: 물리 탭 선택 ──────────────────
        selectAttr(Operator.Attribute.PHYSICAL);

        fadeIn();
    }

    @Override
    public void destroy() {
        Camera.remove(illusCamera);
        super.destroy();
    }

    // ─────────────────────────────────────────────
    // 탭 전환
    // ─────────────────────────────────────────────

    private void selectAttr(Operator.Attribute attr) {
        selectedAttr = attr;
        currentPage = 0;

        for (int i = 0; i < ATTRS.length; i++) {
            tabLines[i].visible = (ATTRS[i] == attr);
            tabBgs[i].alpha(ATTRS[i] == attr ? 1.0f : 0.6f);
        }

        currentAttrOps.clear();
        for (Class<? extends Operator> cls : OperatorRegistry.ALL_OPERATORS) {
            try {
                Operator op = cls.newInstance();
                if (op.attribute() == attr) currentAttrOps.add(cls);
            } catch (Exception e) {
                Game.reportException(e);
            }
        }

        Class<? extends Operator> firstUnlocked = null;
        for (Class<? extends Operator> cls : currentAttrOps) {
            if (OperatorRegistry.isUnlockedAsMain(cls)) {
                firstUnlocked = cls;
                break;
            }
        }
        if (firstUnlocked != null) {
            selectOperator(firstUnlocked);
        } else {
            selectedOpClass = null;
            updateIllustration();
        }

        refreshPortraits();
    }

    // ─────────────────────────────────────────────
    // 오퍼레이터 선택
    // ─────────────────────────────────────────────

    private void selectOperator(Class<? extends Operator> cls) {
        if (!OperatorRegistry.isUnlockedAsMain(cls)) return;
        selectedOpClass = cls;
        updateIllustration();
        refreshPortraits();
    }

    // ─────────────────────────────────────────────
    // 일러스트 + 이름 바 업데이트
    // ─────────────────────────────────────────────

    private void updateIllustration() {
        // 이전 이미지 제거
        if (currentIllus != null) {
            illusGroup.remove(currentIllus);
            currentIllus = null;
        }

        String opName = "—";
        String path   = null;
        int    attrColor = 0xFF888888;

        if (selectedOpClass != null) {
            try {
                Operator op = selectedOpClass.newInstance();
                path      = op.illustration();
                opName    = op.name();
                int ordinal = op.attribute().ordinal();
                if (ordinal < ATTR_COLORS.length) attrColor = ATTR_COLORS[ordinal];
            } catch (Exception e) {
                // fall through to placeholder
            }
        }

        // ── 이름 바 갱신 ────────────────────────────
        nameLabel.text(opName);
        // 좌(정보)·우(진행) 버튼 폭이 같으므로 화면 정중앙에 그대로 정렬한다
        nameLabel.setPos(
            (Camera.main.width - nameLabel.width()) / 2f,
            illusY + illusH + (NAME_BAR_H - nameLabel.height()) / 2f
        );
        align(nameLabel);

        // 선택된 오퍼레이터가 없으면 정보 버튼 비활성
        if (btnInfo != null) {
            btnInfo.enable(selectedOpClass != null);
        }

        nameBarAttrMarker.hardlight(attrColor);

        // ── 일러스트 ───────────────────────────────
        if (path != null) {
            currentIllus = new Image(path);

            // 전신 스플래시 아트(≈1:1)를 넓고 낮은 영역(≈3:1)에 넣기 위해
            // fill+crop 스케일 사용: 영역을 가득 채우고 넘치는 부분은 카메라가 클리핑.
            // (fit으로 축소하면 좌우 여백이 크게 남고 축소율이 커져 디테일이 뭉개진다)
            float scaleX = illusW / currentIllus.width;
            float scaleY = illusH / currentIllus.height;
            float fillScale = Math.max(scaleX, scaleY);
            currentIllus.scale.set(fillScale);

            float scaledW = currentIllus.width  * fillScale;
            float scaledH = currentIllus.height * fillScale;

            // 가로: 중앙 정렬 / 세로: 오퍼레이터별 앵커로 노출 구간 결정
            //   anchorY 0 = 이미지 상단 노출, 0.5 = 중앙, 1 = 하단
            float anchorY = 0.25f;
            if (selectedOpClass != null) {
                try {
                    anchorY = selectedOpClass.newInstance().illustrationAnchorY();
                } catch (Exception e) {
                    // 기본값 유지
                }
            }
            currentIllus.x = (illusW - scaledW) * 0.5f;
            currentIllus.y = (illusH - scaledH) * anchorY;

            // 비정수 배율 축소 시 최근접 필터로 픽셀이 뭉개지는 것을 방지 (일러스트 전용)
            if (currentIllus.texture != null) {
                currentIllus.texture.filter(Texture.LINEAR, Texture.LINEAR);
            }

            illusGroup.add(currentIllus);
            illustrationLabel.visible = false;

        } else {
            illustrationLabel.visible = true;
            illustrationLabel.text(opName);
            illustrationLabel.setPos(
                illusX + (illusW - illustrationLabel.width())  / 2f,
                illusY + (illusH - illustrationLabel.height()) / 2f
            );
            align(illustrationLabel);
        }
    }

    // ─────────────────────────────────────────────
    // 초상화 행 갱신
    // ─────────────────────────────────────────────

    private void refreshPortraits() {
        int total      = currentAttrOps.size();
        int totalPages = (int) Math.ceil((float) total / PORTRAITS_PER_PAGE);
        int startIdx   = currentPage * PORTRAITS_PER_PAGE;

        for (int i = 0; i < PORTRAITS_PER_PAGE; i++) {
            int opIdx = startIdx + i;
            if (opIdx < total) {
                portraitBtns[i].bind(currentAttrOps.get(opIdx));
            } else {
                portraitBtns[i].bind(null);
            }
        }

        btnPrev.visible = btnPrev.active = (currentPage > 0);
        btnNext.visible = btnNext.active = (currentPage < totalPages - 1);
    }

    // ─────────────────────────────────────────────
    // 진행
    // ─────────────────────────────────────────────

    /** 정보 버튼 → 선택된 오퍼레이터의 배틀스킬/연계기/궁극기 팝업 */
    private void showOperatorInfo() {
        if (selectedOpClass == null) return;
        try {
            add(new WndOperatorInfo(selectedOpClass.newInstance()));
        } catch (Exception e) {
            Game.reportException(e);
        }
    }

    private void onProceed() {
        if (selectedOpClass == null) return;
        TeamSelectScene.selectedMain = selectedOpClass;
        ShatteredPixelDungeon.switchNoFade(TeamSelectScene.class);
    }

    @Override
    protected void onBackPressed() {
        ShatteredPixelDungeon.switchNoFade(TitleScene.class);
    }

    // ─────────────────────────────────────────────
    // 속성 라벨 텍스트
    // ─────────────────────────────────────────────

    private static String attrLabel(Operator.Attribute attr) {
        switch (attr) {
            case PHYSICAL: return "물리";
            case HEAT:     return "열기";
            case COLD:     return "냉기";
            case NATURE:   return "자연";
            case ELECTRIC: return "전기";
            default:       return "?";
        }
    }

    // ─────────────────────────────────────────────
    // 초상화 버튼
    // ─────────────────────────────────────────────

    private class PortraitBtn extends Button {

        private Class<? extends Operator> opClass;
        private boolean unlocked;

        private ColorBlock bg;
        private ColorBlock lockOverlay;
        private RenderedTextBlock nameLabel;

        private float lastClickTime = -1f;
        private static final float DOUBLE_CLICK_WINDOW = 0.4f;

        private static final float LIFT = 3f;

        @Override
        protected void createChildren() {
            super.createChildren();

            bg = new ColorBlock(1, 1, 0xFF445544);
            addToBack(bg);

            lockOverlay = new ColorBlock(1, 1, 0xAA000000);
            lockOverlay.visible = false;
            add(lockOverlay);

            nameLabel = renderTextBlock(5);
            nameLabel.hardlight(0xFFFFFF);
            add(nameLabel);
        }

        @Override
        protected void layout() {
            super.layout();

            boolean selected = (opClass != null && opClass == selectedOpClass);
            float yOff = selected ? -LIFT : 0f;

            bg.x = x;
            bg.y = y + yOff;
            bg.size(width, height);

            lockOverlay.x = bg.x;
            lockOverlay.y = bg.y;
            lockOverlay.size(width, height);

            if (nameLabel != null) {
                nameLabel.setPos(
                    x + (width  - nameLabel.width())  / 2f,
                    bg.y + (height - nameLabel.height()) / 2f
                );
                align(nameLabel);
            }
        }

        void bind(Class<? extends Operator> cls) {
            opClass = cls;

            if (cls == null) {
                visible = active = false;
                return;
            }

            visible = active = true;
            unlocked = OperatorRegistry.isUnlockedAsMain(cls);

            try {
                Operator op = cls.newInstance();
                nameLabel.text(op.name());
                // TODO: op.portrait() 로 실제 초상화 이미지 교체 (Phase 3)
            } catch (Exception e) {
                nameLabel.text("?");
            }

            updateAppearance();
            layout();
        }

        private void updateAppearance() {
            if (opClass == null) return;
            boolean selected = (opClass == selectedOpClass);
            bg.brightness(selected ? 1.5f : 0.5f);
            lockOverlay.visible = !unlocked;
        }

        @Override
        public void update() {
            super.update();
            if (opClass == null) return;
            updateAppearance();
            layout();
        }

        @Override
        protected void onClick() {
            if (opClass == null || !unlocked) return;

            float now = Game.timeTotal;
            if (now - lastClickTime < DOUBLE_CLICK_WINDOW) {
                onDoubleClick();
                lastClickTime = -1f;
            } else {
                lastClickTime = now;
                selectOperator(opClass);
            }
        }

        private void onDoubleClick() {
            // TODO: WndOperatorInfo 팝업 구현 (Phase 3)
        }
    }
}
