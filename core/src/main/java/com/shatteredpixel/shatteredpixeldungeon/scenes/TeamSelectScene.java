/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.operators.OperatorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.PointerArea;
import com.watabou.input.PointerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 팀 오퍼레이터 선택 화면 (던전 시작 전, 1번 슬롯).
 *
 * 레이아웃:
 *  - 좌상단: 뒤로가기 버튼 (→ OperatorSelectScene)
 *  - 상단 탭: 속성 필터 5개 (토글 — 전체 ↔ 해당 속성만)
 *  - 좌측: 5열 그리드, 세로 스크롤, 첫 슬롯 = "선택 안 함"
 *  - 우측: 선택된 오퍼레이터 이름 / 연계기 이름 / 연계기 설명
 *  - 우하단: 진행 버튼
 *
 * 메인 오퍼레이터는 OperatorSelectScene.selectedMain 으로 전달받는다.
 */
public class TeamSelectScene extends PixelScene {

    /** OperatorSelectScene → TeamSelectScene 데이터 전달 */
    public static Class<? extends Operator> selectedMain = null;

    // ─── 레이아웃 상수 ─────────────────────────────
    private static final int COLS          = 5;
    private static final int CELL_SIZE     = 36;
    private static final int CELL_GAP      = 3;
    private static final int TAB_HEIGHT    = 16;
    private static final int INFO_MIN_W    = 120;

    // ─── 속성 탭 ──────────────────────────────────
    private static final Operator.Attribute[] ATTRS = Operator.Attribute.values();
    private static final int[] ATTR_COLORS = {
        0xFF888888, 0xFFCC4400, 0xFF4488CC, 0xFF44AA44, 0xFFCCCC00
    };

    /** null = 전체 보기, non-null = 해당 속성만 */
    private Operator.Attribute filterAttr = null;

    // ─── 오퍼레이터 목록 ───────────────────────────
    /** 현재 필터 적용된 표시 목록 (null = "선택 안 함" 슬롯) */
    private List<Class<? extends TeamOperator>> displayList = new ArrayList<>();

    /** 선택된 항목 (null = 선택 안 함) */
    private Class<? extends TeamOperator> selectedOp = null;

    // ─── 스크롤 ────────────────────────────────────
    private int scrollRow = 0;   // 현재 보이는 첫 행 인덱스
    private int visibleRows;

    // ─── UI ───────────────────────────────────────
    private ColorBlock[] tabBgs   = new ColorBlock[ATTRS.length];
    private ColorBlock[] tabLines = new ColorBlock[ATTRS.length];

    private List<CellBtn> cellBtns = new ArrayList<>();

    private RenderedTextBlock infoName;
    private RenderedTextBlock infoChainName;
    private RenderedTextBlock infoChainDesc;
    private ColorBlock infoBg;

    private IconButton btnBack;
    private StyledButton btnProceed;

    // 그리드 영역 좌표 (스크롤 계산용)
    private float gridX, gridY, gridW, gridH;
    private PointerArea scrollArea;

    @Override
    public void create() {
        super.create();

        OperatorRegistry.loadGlobal();

        float W = Camera.main.width;
        float H = Camera.main.height;

        // ── 배경 ──────────────────────────────────
        add(new ColorBlock(W, H, 0xFF1a2a3a));

        // ── 레이아웃 분할 ─────────────────────────
        float tabY     = 4f;
        float contentY = tabY + TAB_HEIGHT + 4f;
        float contentH = H - contentY - 4f;

        gridW = (COLS * CELL_SIZE + (COLS - 1) * CELL_GAP);
        gridX = 4f;
        gridY = contentY;
        gridH = contentH;

        float infoX = gridX + gridW + 8f;
        float infoW = Math.max(W - infoX - 4f, INFO_MIN_W);

        visibleRows = (int)(gridH / (CELL_SIZE + CELL_GAP));

        // ── 속성 탭 ───────────────────────────────
        float tabAreaW = gridW;
        float tabW     = (tabAreaW - (ATTRS.length - 1) * 2f) / ATTRS.length;
        float tabStartX = gridX;

        for (int i = 0; i < ATTRS.length; i++) {
            final Operator.Attribute attr = ATTRS[i];

            ColorBlock bg = new ColorBlock(tabW, TAB_HEIGHT, ATTR_COLORS[i]);
            bg.x = tabStartX + i * (tabW + 2f);
            bg.y = tabY;
            add(bg);
            tabBgs[i] = bg;

            ColorBlock line = new ColorBlock(tabW, 2, 0xFFFFFFFF);
            line.x = bg.x;
            line.y = bg.y + TAB_HEIGHT - 2;
            line.visible = false;
            add(line);
            tabLines[i] = line;

            RenderedTextBlock lbl = renderTextBlock(attrLabel(attr), 6);
            lbl.hardlight(0xFFFFFF);
            lbl.setPos(bg.x + (tabW - lbl.width()) / 2f,
                       bg.y + (TAB_HEIGHT - lbl.height()) / 2f);
            align(lbl);
            add(lbl);

            PointerArea hit = new PointerArea(bg.x, bg.y, tabW, TAB_HEIGHT) {
                @Override
                protected void onClick(PointerEvent event) {
                    toggleFilter(attr);
                }
            };
            add(hit);
        }

        // ── 정보 패널 ─────────────────────────────
        infoBg = new ColorBlock(infoW, contentH, 0xFF2a3a2a);
        infoBg.x = infoX;
        infoBg.y = contentY;
        add(infoBg);

        infoName = renderTextBlock(8);
        infoName.hardlight(Window.TITLE_COLOR);
        add(infoName);

        // 구분선
        ColorBlock divider = new ColorBlock(infoW - 8f, 1, 0xFF556655);
        divider.x = infoX + 4f;
        divider.y = contentY + 20f;
        add(divider);

        infoChainName = renderTextBlock(7);
        infoChainName.hardlight(0xCCFFCC);
        add(infoChainName);

        infoChainDesc = renderTextBlock(6);
        infoChainDesc.hardlight(0xAABBAA);
        add(infoChainDesc);

        // ── 진행 버튼 ─────────────────────────────
        btnProceed = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "진행") {
            @Override
            protected void onClick() { onProceed(); }
        };
        btnProceed.icon(Icons.get(Icons.ENTER));
        btnProceed.setSize(50, 18);
        btnProceed.setPos(infoX + infoW - btnProceed.width() - 4f,
                          contentY + contentH - btnProceed.height() - 4f);
        btnProceed.textColor(Window.TITLE_COLOR);
        add(btnProceed);

        // ── 뒤로가기 버튼 ─────────────────────────
        btnBack = new IconButton(Icons.get(Icons.ARROW)) {
            @Override
            protected void onClick() { onBackPressed(); }
        };
        btnBack.setRect(4f, tabY, 16f, TAB_HEIGHT);
        add(btnBack);

        // ── 스크롤 영역 ───────────────────────────
        scrollArea = new PointerArea(gridX, gridY, gridW, gridH) {
            private float lastY;
            @Override
            protected void onPointerDown(PointerEvent event) { lastY = event.current.y; }
            @Override
            protected void onDrag(PointerEvent event) {
                float dy = lastY - event.current.y;
                lastY = event.current.y;
                if (Math.abs(dy) > 2f) scroll(dy > 0 ? 1 : -1);
            }
        };
        add(scrollArea);

        // ── 셀 버튼 생성 (visibleRows+1 행 미리 생성) ──
        int btnCount = (visibleRows + 1) * COLS;
        for (int i = 0; i < btnCount; i++) {
            CellBtn btn = new CellBtn();
            float bx = gridX + (i % COLS) * (CELL_SIZE + CELL_GAP);
            float by = gridY + (i / COLS) * (CELL_SIZE + CELL_GAP);
            btn.setRect(bx, by, CELL_SIZE, CELL_SIZE);
            add(btn);
            cellBtns.add(btn);
        }

        // ── 초기 상태 ─────────────────────────────
        buildDisplayList();
        selectOp(null);  // "선택 안 함" 기본 선택
        refreshGrid();

        fadeIn();
    }

    // ─────────────────────────────────────────────
    // 속성 필터 토글
    // ─────────────────────────────────────────────

    private void toggleFilter(Operator.Attribute attr) {
        if (filterAttr == attr) {
            filterAttr = null;  // 같은 탭 재클릭 → 전체 보기
        } else {
            filterAttr = attr;
        }
        for (int i = 0; i < ATTRS.length; i++) {
            tabLines[i].visible = (ATTRS[i] == filterAttr);
        }
        scrollRow = 0;
        buildDisplayList();
        refreshGrid();
    }

    // ─────────────────────────────────────────────
    // 표시 목록 구성
    // ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void buildDisplayList() {
        displayList.clear();
        displayList.add(null); // 첫 슬롯 = "선택 안 함"

        for (Class<? extends Operator> cls : OperatorRegistry.ALL_OPERATORS) {
            if (!TeamOperator.class.isAssignableFrom(cls)) continue;
            if (cls == selectedMain) continue; // 메인으로 선택한 오퍼레이터 제외

            if (filterAttr != null) {
                try {
                    Operator op = cls.newInstance();
                    if (op.attribute() != filterAttr) continue;
                } catch (Exception e) {
                    Game.reportException(e);
                    continue;
                }
            }
            displayList.add((Class<? extends TeamOperator>) cls);
        }
    }

    // ─────────────────────────────────────────────
    // 그리드 갱신
    // ─────────────────────────────────────────────

    private void refreshGrid() {
        int startIdx = scrollRow * COLS;

        for (int i = 0; i < cellBtns.size(); i++) {
            int dataIdx = startIdx + i;
            if (dataIdx < displayList.size()) {
                cellBtns.get(i).bind(displayList.get(dataIdx), dataIdx == 0);
            } else {
                cellBtns.get(i).bind(null, false);
            }
        }
    }

    // ─────────────────────────────────────────────
    // 스크롤
    // ─────────────────────────────────────────────

    private void scroll(int delta) {
        int totalRows = (int) Math.ceil((float) displayList.size() / COLS);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        scrollRow = Math.max(0, Math.min(scrollRow + delta, maxScroll));
        refreshGrid();
    }

    // ─────────────────────────────────────────────
    // 오퍼레이터 선택
    // ─────────────────────────────────────────────

    private void selectOp(Class<? extends TeamOperator> cls) {
        selectedOp = cls;
        updateInfo();
        refreshGrid();
    }

    private void updateInfo() {
        float ix = infoBg.x + 4f;
        float iy = infoBg.y + 4f;
        float iw = infoBg.width - 8f;

        if (selectedOp == null) {
            infoName.text("선택 안 함");
            infoName.maxWidth((int) iw);
            infoName.setPos(ix, iy);
            align(infoName);

            infoChainName.text("");
            infoChainDesc.text("");
            return;
        }

        try {
            TeamOperator op = (TeamOperator) selectedOp.newInstance();
            infoName.text(op.name());
            infoName.maxWidth((int) iw);
            infoName.setPos(ix, iy);
            align(infoName);

            infoChainName.text(op.chainName());
            infoChainName.maxWidth((int) iw);
            infoChainName.setPos(ix, infoName.bottom() + 8f);
            align(infoChainName);

            infoChainDesc.text(op.chainDescription());
            infoChainDesc.maxWidth((int) iw);
            infoChainDesc.setPos(ix, infoChainName.bottom() + 3f);
            align(infoChainDesc);

        } catch (Exception e) {
            Game.reportException(e);
        }
    }

    // ─────────────────────────────────────────────
    // 진행
    // ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void onProceed() {
        // GamesInProgress에 선택 정보 저장 → Dungeon.init()에서 읽음
        GamesInProgress.selectedMainOp = (Class<? extends Operator>) selectedMain;
        GamesInProgress.selectedTeamOp = selectedOp;
        GamesInProgress.curSlot = 1; // 슬롯 고정 (추후 멀티슬롯 지원 시 수정)

        Dungeon.hero = null;
        Dungeon.daily = Dungeon.dailyReplay = false;
        Dungeon.initSeed();
        ActionIndicator.clearAction();
        InterlevelScene.mode = InterlevelScene.Mode.DESCEND;

        Game.switchScene(InterlevelScene.class);
    }

    @Override
    protected void onBackPressed() {
        ShatteredPixelDungeon.switchNoFade(OperatorSelectScene.class);
    }

    // ─────────────────────────────────────────────
    // 속성 라벨
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
    // 셀 버튼
    // ─────────────────────────────────────────────

    private class CellBtn extends Button {

        private Class<? extends TeamOperator> opClass;
        private boolean isNoneSlot;
        private boolean unlocked;

        private ColorBlock bg;
        private ColorBlock lockOverlay;
        private RenderedTextBlock label;

        private static final float LIFT = 3f;

        @Override
        protected void createChildren() {
            super.createChildren();

            bg = new ColorBlock(1, 1, 0xFF334433);
            addToBack(bg);

            lockOverlay = new ColorBlock(1, 1, 0xAA000000);
            lockOverlay.visible = false;
            add(lockOverlay);

            label = renderTextBlock(5);
            label.hardlight(0xFFFFFF);
            add(label);
        }

        @Override
        protected void layout() {
            super.layout();
            boolean selected = isSelected();
            float yOff = selected ? -LIFT : 0f;

            bg.x = x; bg.y = y + yOff;
            bg.size(width, height);
            lockOverlay.x = bg.x; lockOverlay.y = bg.y;
            lockOverlay.size(width, height);

            if (label != null) {
                label.setPos(x + (width - label.width()) / 2f,
                             bg.y + (height - label.height()) / 2f);
                align(label);
            }
        }

        void bind(Class<? extends TeamOperator> cls, boolean noneSlot) {
            if (cls == null && !noneSlot) {
                visible = active = false;
                return;
            }
            visible = active = true;
            opClass   = cls;
            isNoneSlot = noneSlot;
            unlocked   = noneSlot || OperatorRegistry.isUnlockedAsMain(cls);

            if (noneSlot) {
                label.text("✕");
            } else {
                try {
                    label.text(cls.newInstance().name());
                } catch (Exception e) {
                    label.text("?");
                }
            }

            updateAppearance();
            layout();
        }

        private boolean isSelected() {
            if (isNoneSlot) return selectedOp == null;
            return opClass != null && opClass == selectedOp;
        }

        private void updateAppearance() {
            bg.brightness(isSelected() ? 1.5f : 0.5f);
            lockOverlay.visible = !unlocked;
        }

        @Override
        public void update() {
            super.update();
            if (!visible) return;
            updateAppearance();
            layout();
        }

        @Override
        protected void onClick() {
            if (!unlocked) return;
            if (isNoneSlot) {
                selectOp(null);
            } else if (opClass != null) {
                selectOp(opClass);
            }
        }
    }
}
