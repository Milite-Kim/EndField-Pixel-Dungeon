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
import com.watabou.noosa.PointerArea;

import java.util.ArrayList;
import java.util.List;

/**
 * 메인 오퍼레이터 선택 화면.
 *
 * 레이아웃:
 *  - 상단: 속성 탭 5개
 *  - 중앙: 선택된 오퍼레이터 일러스트 (placeholder: 색상 블록 + 이름)
 *  - 하단: 초상화 행 (PORTRAITS_PER_PAGE 개씩, 페이지 이동 가능)
 *  - 일러스트 위 우하단: 진행 버튼
 *  - 좌상단: 뒤로가기 버튼
 */
public class OperatorSelectScene extends PixelScene {

    // 한 페이지에 보여줄 초상화 수
    private static final int PORTRAITS_PER_PAGE = 7;

    // 초상화 크기/간격
    private static final int PORTRAIT_SIZE   = 28;
    private static final int PORTRAIT_GAP    = 2;

    // 탭 크기
    private static final int TAB_HEIGHT = 16;

    // 속성 탭 색상 (placeholder — 아이콘으로 교체 예정)
    private static final int[] ATTR_COLORS = {
        0xFF888888, // PHYSICAL  물리
        0xFFCC4400, // FIRE      열기
        0xFF4488CC, // COLD      냉기
        0xFF44AA44, // NATURE    자연
        0xFFCCCC00, // ELECTRIC  전기
    };

    private static final Operator.Attribute[] ATTRS = Operator.Attribute.values();

    // ─── 상태 ───────────────────────────────────
    private Operator.Attribute selectedAttr = Operator.Attribute.PHYSICAL;
    private Class<? extends Operator> selectedOpClass = Endministrator.class;
    private int currentPage = 0;

    // 현재 탭의 오퍼레이터 목록 (해금/미해금 모두 포함, 순서 고정)
    private List<Class<? extends Operator>> currentAttrOps = new ArrayList<>();

    // ─── UI 요소 ─────────────────────────────────
    private ColorBlock illustrationBg;
    private RenderedTextBlock illustrationLabel;  // placeholder 텍스트

    private ColorBlock[] tabBgs   = new ColorBlock[ATTRS.length];
    private ColorBlock[] tabLines = new ColorBlock[ATTRS.length]; // 선택 표시 밑줄
    private RenderedTextBlock[] tabLabels = new RenderedTextBlock[ATTRS.length];

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

        // ── 배경 ──────────────────────────────────
        ColorBlock sceneBg = new ColorBlock(W, H, 0xFF1a2a3a);
        add(sceneBg);

        // ── 속성 탭 ───────────────────────────────
        int tabCount   = ATTRS.length;
        float tabAreaW = W * 0.7f;
        float tabW     = (tabAreaW - (tabCount - 1) * 2f) / tabCount;
        float tabStartX = (W - tabAreaW) / 2f;
        float tabY      = 4f;

        for (int i = 0; i < tabCount; i++) {
            final int idx = i;
            final Operator.Attribute attr = ATTRS[i];

            ColorBlock bg = new ColorBlock(tabW, TAB_HEIGHT, ATTR_COLORS[i]);
            bg.x = tabStartX + i * (tabW + 2);
            bg.y = tabY;
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

            // 탭 클릭 영역
            com.watabou.noosa.PointerArea hit = new com.watabou.noosa.PointerArea(
                    bg.x, bg.y, tabW, TAB_HEIGHT) {
                @Override
                protected void onClick(com.watabou.input.PointerEvent event) {
                    selectAttr(attr);
                }
            };
            add(hit);
        }

        // ── 일러스트 영역 ─────────────────────────
        float illusX = W * 0.12f;
        float illusY = tabY + TAB_HEIGHT + 4f;
        float illusW = W * 0.76f;
        float illusH = H - illusY - PORTRAIT_SIZE - PORTRAIT_GAP * 2 - 6f;

        illustrationBg = new ColorBlock(illusW, illusH, 0xFF2d3d2d);
        illustrationBg.x = illusX;
        illustrationBg.y = illusY;
        add(illustrationBg);

        illustrationLabel = renderTextBlock("", 9);
        illustrationLabel.hardlight(0xCCFFCC);
        add(illustrationLabel);

        // ── 진행 버튼 (일러스트 위에 float) ──────────
        btnProceed = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "진행") {
            @Override
            protected void onClick() {
                onProceed();
            }
        };
        btnProceed.icon(Icons.get(Icons.ENTER));
        btnProceed.setSize(50, 18);
        btnProceed.setPos(illusX + illusW - btnProceed.width() - 4,
                          illusY + illusH - btnProceed.height() - 4);
        btnProceed.textColor(Window.TITLE_COLOR);
        add(btnProceed);

        // ── 하단 초상화 버튼 ───────────────────────
        float portraitRowY = illusY + illusH + PORTRAIT_GAP + 2f;
        float portraitRowX = illusX;

        for (int i = 0; i < PORTRAITS_PER_PAGE; i++) {
            PortraitBtn btn = new PortraitBtn();
            btn.setRect(portraitRowX + i * (PORTRAIT_SIZE + PORTRAIT_GAP),
                        portraitRowY, PORTRAIT_SIZE, PORTRAIT_SIZE);
            add(btn);
            portraitBtns[i] = btn;
        }

        // ── 이전/다음 페이지 버튼 ─────────────────
        float arrowY = portraitRowY + (PORTRAIT_SIZE - 16) / 2f;

        btnPrev = new IconButton(Icons.get(Icons.LEFTARROW)) {
            @Override
            protected void onClick() {
                currentPage--;
                refreshPortraits();
            }
        };
        btnPrev.setRect(illusX - 18, arrowY, 16, 16);
        add(btnPrev);

        btnNext = new IconButton(Icons.get(Icons.RIGHTARROW)) {
            @Override
            protected void onClick() {
                currentPage++;
                refreshPortraits();
            }
        };
        btnNext.setRect(illusX + illusW + 2, arrowY, 16, 16);
        add(btnNext);

        // ── 뒤로가기 버튼 ─────────────────────────
        btnBack = new IconButton(Icons.get(Icons.ARROW)) {
            @Override
            protected void onClick() {
                onBackPressed();
            }
        };
        btnBack.setRect(4, 4, 16, 16);
        add(btnBack);

        // ── 탭 라벨 위치 정렬 ────────────────────
        for (int i = 0; i < tabCount; i++) {
            ColorBlock bg = tabBgs[i];
            RenderedTextBlock lbl = tabLabels[i];
            lbl.setPos(bg.x + (tabW - lbl.width()) / 2f,
                       bg.y + (TAB_HEIGHT - lbl.height()) / 2f);
            align(lbl);
        }

        // ── 초기 상태: 물리 탭 선택 ──────────────
        selectAttr(Operator.Attribute.PHYSICAL);

        fadeIn();
    }

    // ─────────────────────────────────────────────
    // 탭 전환
    // ─────────────────────────────────────────────

    private void selectAttr(Operator.Attribute attr) {
        selectedAttr = attr;
        currentPage = 0;

        // 탭 밑줄 갱신
        for (int i = 0; i < ATTRS.length; i++) {
            tabLines[i].visible = (ATTRS[i] == attr);
        }

        // 해당 속성 오퍼레이터 수집 (ALL_OPERATORS 순서 유지)
        currentAttrOps.clear();
        for (Class<? extends Operator> cls : OperatorRegistry.ALL_OPERATORS) {
            try {
                Operator op = cls.newInstance();
                if (op.attribute() == attr) currentAttrOps.add(cls);
            } catch (Exception e) {
                Game.reportException(e);
            }
        }

        // 해금된 첫 번째 오퍼레이터를 기본 선택
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

    private void updateIllustration() {
        if (selectedOpClass == null) {
            illustrationLabel.text("—");
        } else {
            try {
                Operator op = selectedOpClass.newInstance();
                // TODO: op.illustration() 로 실제 이미지 교체 (Phase 3)
                illustrationLabel.text(op.name());
            } catch (Exception e) {
                illustrationLabel.text("?");
            }
        }
        // 일러스트 영역 중앙 정렬
        illustrationLabel.setPos(
            illustrationBg.x + (illustrationBg.width  - illustrationLabel.width())  / 2f,
            illustrationBg.y + (illustrationBg.height - illustrationLabel.height()) / 2f
        );
        align(illustrationLabel);
    }

    // ─────────────────────────────────────────────
    // 초상화 행 갱신
    // ─────────────────────────────────────────────

    private void refreshPortraits() {
        int total     = currentAttrOps.size();
        int totalPages = (int) Math.ceil((float) total / PORTRAITS_PER_PAGE);
        int startIdx  = currentPage * PORTRAITS_PER_PAGE;

        for (int i = 0; i < PORTRAITS_PER_PAGE; i++) {
            int opIdx = startIdx + i;
            if (opIdx < total) {
                portraitBtns[i].bind(currentAttrOps.get(opIdx));
            } else {
                portraitBtns[i].bind(null);
            }
        }

        // 페이지 버튼 표시 여부
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
            case FIRE:     return "열기";
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

        // placeholder 배경 + 이름 텍스트
        private ColorBlock bg;
        private ColorBlock lockOverlay;
        private RenderedTextBlock nameLabel;

        // 더블클릭 감지
        private float lastClickTime = -1f;
        private static final float DOUBLE_CLICK_WINDOW = 0.4f;

        // 선택 시 위로 올라오는 오프셋
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
            // ShatteredPixelDungeon.scene().addToFront(new WndOperatorInfo(opClass));
        }
    }
}
