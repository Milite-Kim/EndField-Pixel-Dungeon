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

        // ── 일러스트 영역: 전체 너비 × 나머지 높이 ──────
        illusX = 0f;
        illusY = 0f;
        illusW = W;
        illusH = H - bottomH;

        illustrationBg = new ColorBlock(illusW, illusH, 0xFF1a1a2a);
        illustrationBg.x = 0f;
        illustrationBg.y = 0f;
        add(illustrationBg);

        // ── 일러스트 카메라 + Group ──────────────────
        Point camP = Camera.main.cameraToScreen(0f, 0f);
        illusCamera = new Camera(camP.x, camP.y, (int)illusW, (int)illusH, defaultZoom);
        Camera.add(illusCamera);

        illusGroup = new Group();
        illusGroup.camera = illusCamera;
        add(illusGroup);

        // ── placeholder 텍스트 ─────────────────────
        illustrationLabel = renderTextBlock("", 9);
        illustrationLabel.hardlight(0xCCFFCC);
        add(illustrationLabel);

        // ── 일러스트 하단 페이드 (이름 바와 자연스럽게 이어지도록) ──
        ColorBlock bottomFade = new ColorBlock(W, Math.min(60f, illusH * 0.25f), 0xCC111820);
        bottomFade.x = 0f;
        bottomFade.y = illusH - bottomFade.height;
        add(bottomFade);

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
        float nameBarY = illusH;
        ColorBlock nameBarBg = new ColorBlock(W, NAME_BAR_H, 0xEE1a2a3a);
        nameBarBg.x = 0f;
        nameBarBg.y = nameBarY;
        add(nameBarBg);

        // 속성 색상 마커 (이름 바 좌측) — 흰색 기반 + hardlight로 색상 동적 교체
        nameBarAttrMarker = new ColorBlock(3f, NAME_BAR_H - 4f, 0xFFFFFFFF);
        nameBarAttrMarker.x = 6f;
        nameBarAttrMarker.y = nameBarY + 2f;
        add(nameBarAttrMarker);

        // 오퍼레이터 이름 (이름 바 중앙)
        nameLabel = renderTextBlock("", 9);
        nameLabel.hardlight(Window.TITLE_COLOR);
        add(nameLabel);

        // ── 진행 버튼 (이름 바 우측) ──────────────────
        btnProceed = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "진행") {
            @Override
            protected void onClick() {
                onProceed();
            }
        };
        btnProceed.icon(Icons.get(Icons.ENTER));
        btnProceed.setSize(50, NAME_BAR_H - 6f);
        btnProceed.setPos(W - btnProceed.width() - 4f,
                          nameBarY + (NAME_BAR_H - btnProceed.height()) / 2f);
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

        // ── 뒤로가기 버튼 (좌상단, 탭 위에 오버레이) ──────
        btnBack = new IconButton(Icons.get(Icons.ARROW)) {
            @Override
            protected void onClick() {
                onBackPressed();
            }
        };
        btnBack.setRect(2f, (TAB_HEIGHT - 14f) / 2f + 1f, 16, 16);
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
        // 이름 바 중앙 정렬 (진행 버튼 왼쪽 공간)
        float nameCenterX = (Camera.main.width - btnProceed.width() - 8f) / 2f;
        nameLabel.setPos(
            nameCenterX - nameLabel.width() / 2f,
            illusH + (NAME_BAR_H - nameLabel.height()) / 2f
        );
        align(nameLabel);

        nameBarAttrMarker.hardlight(attrColor);

        // ── 일러스트 ───────────────────────────────
        if (path != null) {
            currentIllus = new Image(path);

            // fill 스케일: 영역을 완전히 덮도록 (넘치는 부분은 카메라 클리핑)
            float scaleX = illusW / currentIllus.width;
            float scaleY = illusH / currentIllus.height;
            float fillScale = Math.max(scaleX, scaleY);
            currentIllus.scale.set(fillScale);

            // 중앙 정렬 (illusCamera 좌표계 기준)
            currentIllus.x = (illusW - currentIllus.width  * fillScale) / 2f;
            currentIllus.y = (illusH - currentIllus.height * fillScale) / 2f;

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
