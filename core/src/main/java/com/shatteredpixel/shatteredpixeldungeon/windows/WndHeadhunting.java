/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.HeadhuntingPermit;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.operators.OperatorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * 헤드헌팅 허가증 사용 시 표시되는 UI 창.
 *
 * ■ 패널 1 (메인): 4개 다이아몬드 + 리롤 버튼 + 포기 버튼
 * ■ 패널 2 (디테일): 선택한 오퍼레이터 이름·연계기 정보 + 영입하기 버튼
 * ■ 패널 3 (교체): 파티 만원 시 교체할 오퍼레이터 선택
 */
public class WndHeadhunting extends Window {

    // ─── 레이아웃 상수 ─────────────────────────────
    private static final int W_P     = 130;
    private static final int W_L     = 160;
    private static final int H_P     = 158;
    private static final int H_L     = 130;

    /** 다이아몬드 셀 바운딩 박스 (픽셀) */
    private static final int CELL    = 48;
    /** ColorBlock 한 변 길이. 45° 회전 후 대각선 ≈ 34√2 ≈ 48px */
    private static final int DIA_S   = 34;
    private static final int COL_GAP = 4;
    private static final int ROW_GAP = 4;
    private static final int MARGIN  = 4;
    private static final int BTN_H   = 14;

    // ─── 상태 ──────────────────────────────────────
    private final Hero hero;
    private final HeadhuntingPermit permit;
    private List<Class<? extends Operator>> candidates;
    private Class<? extends Operator> selected;
    private int rerollsLeft;
    private final int W;
    private final int H;

    // ─── 패널 ──────────────────────────────────────
    private Group panelMain   = new Group();
    private Group panelDetail = new Group();
    private Group panelSwap   = new Group();

    // ─── 메인 패널 참조 ────────────────────────────
    private final DiamondBtn[] diamondBtns = new DiamondBtn[HeadhuntingPermit.CANDIDATES];
    private RedButton btnReroll;

    // ─── 디테일 패널 참조 ──────────────────────────
    private RenderedTextBlock detailOpName;
    private ColorBlock detailDivider;
    private RenderedTextBlock detailChainName;
    private RenderedTextBlock detailChainDesc;

    // ─── 교체 패널 참조 ────────────────────────────
    private float swapBtnStartY;
    private final List<RedButton> swapBtns = new ArrayList<>();

    // ─────────────────────────────────────────────
    // 생성자
    // ─────────────────────────────────────────────

    public WndHeadhunting(Hero hero, HeadhuntingPermit permit, List<Class<? extends Operator>> candidates) {
        super();
        this.hero       = hero;
        this.permit     = permit;
        this.candidates = new ArrayList<>(candidates);
        this.rerollsLeft = HeadhuntingPermit.MAX_REROLLS;
        this.W = PixelScene.landscape() ? W_L : W_P;
        this.H = PixelScene.landscape() ? H_L : H_P;

        buildMainPanel();
        buildDetailPanel();
        buildSwapPanel();

        add(panelMain);
        add(panelDetail);
        add(panelSwap);

        showPanel(panelMain);
        resize(W, H);
    }

    // ─────────────────────────────────────────────
    // 패널 전환
    // ─────────────────────────────────────────────

    private void showPanel(Group panel) {
        panelMain.visible   = panelMain.active   = (panel == panelMain);
        panelDetail.visible = panelDetail.active = (panel == panelDetail);
        panelSwap.visible   = panelSwap.active   = (panel == panelSwap);
    }

    // ─────────────────────────────────────────────
    // 패널 1 — 메인 (다이아몬드 4개)
    // ─────────────────────────────────────────────

    private void buildMainPanel() {
        // ── 제목 ──────────────────────────────────
        RenderedTextBlock title = PixelScene.renderTextBlock("헤드헌팅", 9);
        title.hardlight(Window.TITLE_COLOR);
        title.setPos((W - title.width()) / 2f, MARGIN);
        PixelScene.align(title);
        panelMain.add(title);

        float y0 = title.bottom() + 4f;

        // ── 리롤 버튼 (좌측) ──────────────────────
        btnReroll = new RedButton("↺ 리롤") {
            @Override
            protected void onClick() { onReroll(); }
        };
        btnReroll.setRect(MARGIN, y0, 36, BTN_H);
        updateRerollBtn();
        panelMain.add(btnReroll);

        // ── 2×2 다이아몬드 그리드 ─────────────────
        float gridW = 2 * CELL + COL_GAP;
        float gridX = (W - gridW) / 2f;
        float gridY = y0 + BTN_H + 4f;

        for (int i = 0; i < HeadhuntingPermit.CANDIDATES; i++) {
            int col = i % 2;
            int row = i / 2;
            float cx = gridX + col * (CELL + COL_GAP);
            float cy = gridY + row * (CELL + ROW_GAP);

            DiamondBtn btn = new DiamondBtn();
            btn.setRect(cx, cy, CELL, CELL);
            panelMain.add(btn);
            diamondBtns[i] = btn;
        }
        refreshDiamonds();

        // ── 포기 버튼 (우하단) ────────────────────
        float abandonY = gridY + 2 * CELL + ROW_GAP + 4f;
        RedButton btnAbandon = new RedButton("포기") {
            @Override
            protected void onClick() { onAbandon(); }
        };
        btnAbandon.setRect(W - 40 - MARGIN, abandonY, 40, BTN_H);
        panelMain.add(btnAbandon);
    }

    /** 후보 목록을 다이아몬드 버튼에 반영 */
    private void refreshDiamonds() {
        for (int i = 0; i < HeadhuntingPermit.CANDIDATES; i++) {
            Class<? extends Operator> cls = (i < candidates.size()) ? candidates.get(i) : null;
            diamondBtns[i].bind(cls);
        }
    }

    private void updateRerollBtn() {
        if (btnReroll != null) {
            btnReroll.enable(rerollsLeft > 0);
        }
    }

    // ─────────────────────────────────────────────
    // 패널 2 — 디테일
    // ─────────────────────────────────────────────

    private void buildDetailPanel() {
        // ── 뒤로가기 ──────────────────────────────
        RedButton btnBack = new RedButton("←") {
            @Override
            protected void onClick() { showPanel(panelMain); }
        };
        btnBack.setRect(MARGIN, MARGIN, 24, BTN_H);
        panelDetail.add(btnBack);

        // ── 오퍼레이터 이름 ───────────────────────
        detailOpName = PixelScene.renderTextBlock(9);
        detailOpName.hardlight(Window.TITLE_COLOR);
        detailOpName.maxWidth(W - MARGIN * 2);
        detailOpName.setPos(MARGIN, btnBack.bottom() + 4f);
        PixelScene.align(detailOpName);
        panelDetail.add(detailOpName);

        // ── 구분선 ────────────────────────────────
        detailDivider = new ColorBlock(W - MARGIN * 2, 1, 0xFF445544);
        detailDivider.x = MARGIN;
        detailDivider.y = detailOpName.bottom() + 3f;
        panelDetail.add(detailDivider);

        // ── 연계기 이름 ───────────────────────────
        detailChainName = PixelScene.renderTextBlock(7);
        detailChainName.hardlight(0xCCFFCC);
        detailChainName.maxWidth(W - MARGIN * 2);
        detailChainName.setPos(MARGIN, detailDivider.y + 5f);
        PixelScene.align(detailChainName);
        panelDetail.add(detailChainName);

        // ── 연계기 설명 ───────────────────────────
        detailChainDesc = PixelScene.renderTextBlock(6);
        detailChainDesc.hardlight(0xAABBAA);
        detailChainDesc.maxWidth(W - MARGIN * 2);
        detailChainDesc.setPos(MARGIN, detailChainName.bottom() + 3f);
        PixelScene.align(detailChainDesc);
        panelDetail.add(detailChainDesc);

        // ── 영입하기 버튼 (하단 고정) ─────────────
        RedButton btnRecruit = new RedButton("영입하기") {
            @Override
            protected void onClick() { onRecruit(); }
        };
        btnRecruit.setRect((W - 60) / 2f, H - BTN_H - MARGIN, 60, BTN_H);
        panelDetail.add(btnRecruit);
    }

    /** 선택된 오퍼레이터 정보로 디테일 패널 내용 갱신 */
    private void updateDetailPanel() {
        if (selected == null) return;
        try {
            Operator op = selected.newInstance();

            // 이름
            detailOpName.text(op.name());
            detailOpName.maxWidth(W - MARGIN * 2);
            detailOpName.setPos(MARGIN, MARGIN + BTN_H + 4f);
            PixelScene.align(detailOpName);

            // 구분선 위치 갱신
            detailDivider.y = detailOpName.bottom() + 3f;

            // 연계기 정보
            if (op instanceof TeamOperator) {
                TeamOperator teamOp = (TeamOperator) op;
                detailChainName.text(teamOp.chainName());
                detailChainDesc.text(teamOp.chainDescription());
            } else {
                detailChainName.text("(팀 연계 없음)");
                detailChainDesc.text("");
            }

            detailChainName.maxWidth(W - MARGIN * 2);
            detailChainName.setPos(MARGIN, detailDivider.y + 5f);
            PixelScene.align(detailChainName);

            detailChainDesc.maxWidth(W - MARGIN * 2);
            detailChainDesc.setPos(MARGIN, detailChainName.bottom() + 3f);
            PixelScene.align(detailChainDesc);

        } catch (Exception e) {
            Game.reportException(e);
        }
    }

    // ─────────────────────────────────────────────
    // 패널 3 — 교체
    // ─────────────────────────────────────────────

    private void buildSwapPanel() {
        // ── 뒤로가기 ──────────────────────────────
        RedButton btnBack = new RedButton("←") {
            @Override
            protected void onClick() { showPanel(panelDetail); }
        };
        btnBack.setRect(MARGIN, MARGIN, 24, BTN_H);
        panelSwap.add(btnBack);

        // ── 안내 메시지 ───────────────────────────
        RenderedTextBlock msg = PixelScene.renderTextBlock("교체할 오퍼레이터를\n선택하세요", 7);
        msg.hardlight(0xFFFFFF);
        msg.maxWidth(W - MARGIN * 2);
        msg.setPos(MARGIN, btnBack.bottom() + 4f);
        PixelScene.align(msg);
        panelSwap.add(msg);

        swapBtnStartY = msg.bottom() + 6f;
    }

    /** 현재 파티 목록으로 교체 버튼들을 동적 갱신 후 패널 표시 */
    private void openSwapPanel() {
        // 기존 동적 버튼 제거
        for (RedButton btn : swapBtns) {
            btn.destroy();
        }
        swapBtns.clear();

        float y = swapBtnStartY;
        for (TeamOperator existing : hero.teamOperators) {
            final TeamOperator target = existing;
            RedButton btn = new RedButton(existing.name()) {
                @Override
                protected void onClick() { doSwap(target); }
            };
            btn.setRect(MARGIN, y, W - MARGIN * 2, BTN_H);
            panelSwap.add(btn);
            swapBtns.add(btn);
            // y를 btn.bottom() + 2f 로 갱신하려면 btn의 bottom 필요
            // RedButton은 setRect 후 bottom() = y + BTN_H
        }
        // 수동으로 y 증가 (BTN_H + 2 간격)
        // (위 루프에서 btn이 추가된 직후 layout이 업데이트되지 않을 수 있으므로
        //  y를 swapBtnStartY 기준으로 직접 계산)
        float btnY = swapBtnStartY;
        for (RedButton btn : swapBtns) {
            btn.setRect(MARGIN, btnY, W - MARGIN * 2, BTN_H);
            btnY += BTN_H + 2;
        }

        showPanel(panelSwap);
    }

    // ─────────────────────────────────────────────
    // 이벤트 핸들러
    // ─────────────────────────────────────────────

    private void onSelectCandidate(Class<? extends Operator> opClass) {
        selected = opClass;
        updateDetailPanel();
        showPanel(panelDetail);
    }

    private void onRecruit() {
        if (selected == null) return;

        if (!(TeamOperator.class.isAssignableFrom(selected))) {
            // 선택된 오퍼레이터가 팀 오퍼레이터가 아님 (현재 구현상 발생하지 않음)
            hide();
            return;
        }

        if (hero.teamOperators.size() < Hero.MAX_TEAM_SIZE) {
            // 빈 슬롯에 바로 추가
            try {
                TeamOperator newOp = (TeamOperator) selected.newInstance();
                hero.addTeamOperator(newOp);
                permit.detach(hero.belongings);
                hide();
            } catch (Exception e) {
                Game.reportException(e);
            }
        } else {
            // 파티 만원 → 교체 패널
            openSwapPanel();
        }
    }

    private void doSwap(TeamOperator oldOp) {
        if (selected == null) return;
        try {
            TeamOperator newOp = (TeamOperator) selected.newInstance();
            hero.replaceTeamOperator(oldOp, newOp);
            permit.detach(hero.belongings);
            hide();
        } catch (Exception e) {
            Game.reportException(e);
        }
    }

    private void onReroll() {
        if (rerollsLeft <= 0) return;
        rerollsLeft--;
        updateRerollBtn();

        // 새 후보 선정 (Fisher-Yates)
        List<Class<? extends Operator>> pool = OperatorRegistry.getRecruitPool(hero);
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = Random.Int(i + 1);
            Class<? extends Operator> tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
        }
        candidates = new ArrayList<>(
                pool.subList(0, Math.min(HeadhuntingPermit.CANDIDATES, pool.size())));

        refreshDiamonds();
    }

    private void onAbandon() {
        hide();
    }

    // ─────────────────────────────────────────────
    // 다이아몬드 버튼
    // ─────────────────────────────────────────────

    /**
     * 오퍼레이터 1명을 나타내는 회전된 정사각형(다이아몬드) 버튼.
     * 45° 회전한 ColorBlock 위에 오퍼레이터 이름 레이블을 표시한다.
     */
    private class DiamondBtn extends Button {

        private Class<? extends Operator> opClass;
        private ColorBlock diamond;
        private RenderedTextBlock label;

        @Override
        protected void createChildren() {
            super.createChildren();

            // 45° 회전으로 다이아몬드 형태 연출
            diamond = new ColorBlock(DIA_S, DIA_S, 0xFF111111);
            diamond.origin.set(DIA_S / 2f, DIA_S / 2f);
            diamond.angle = 45;
            add(diamond);

            label = PixelScene.renderTextBlock(5);
            label.hardlight(0xFFFFFF);
            add(label);
        }

        @Override
        protected void layout() {
            super.layout();
            if (diamond == null) return;

            // 다이아몬드를 셀 중앙에 배치
            diamond.x = x + (width - DIA_S) / 2f;
            diamond.y = y + (height - DIA_S) / 2f;

            // 레이블도 중앙 정렬
            if (label != null) {
                label.setPos(
                        x + (width - label.width()) / 2f,
                        y + (height - label.height()) / 2f);
                PixelScene.align(label);
            }
        }

        /** opClass가 null이면 버튼 숨김, 아니면 갱신 */
        void bind(Class<? extends Operator> cls) {
            opClass = cls;
            if (cls == null) {
                visible = active = false;
                return;
            }
            visible = active = true;
            try {
                label.text(cls.newInstance().name());
            } catch (Exception e) {
                label.text("?");
            }
            layout();
        }

        @Override
        protected void onClick() {
            if (opClass != null) onSelectCandidate(opClass);
        }
    }
}
