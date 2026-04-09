/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.ChainQueue;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

/**
 * 전투 HUD — 배틀스킬 / 궁극기 / 연계기 버튼 + 충전 버튼 + 타겟팅 취소 버튼.
 *
 * 레이아웃:
 *   [연계] [스킬] [궁극]          ← 툴바 바로 위, 화면 가운데 정렬
 *   [충전]                       ← 아츠유닛 충전 > 0 일 때만 표시, 왼쪽 기준
 *          [취소]                 ← 타겟팅 모드일 때만 표시, 스킬/궁 버튼 위
 */
public class CombatHUD extends Component {

    public static final int BTN_SIZE = 28;
    public static final int GAP      = 3;

    private SkillBtn      skillBtn;
    private UltBtn        ultBtn;
    private ChainBtn      chainBtn;
    private ArtsChargeBtn artsChargeBtn;
    private CancelBtn     cancelBtn;

    private static CombatHUD instance;

    public CombatHUD() {
        super();
        instance = this;
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        if (instance == this) instance = null;
    }

    @Override
    protected void createChildren() {
        chainBtn      = new ChainBtn();      add(chainBtn);
        skillBtn      = new SkillBtn();      add(skillBtn);
        ultBtn        = new UltBtn();        add(ultBtn);
        artsChargeBtn = new ArtsChargeBtn(); add(artsChargeBtn);
        cancelBtn     = new CancelBtn();     add(cancelBtn);
    }

    @Override
    protected void layout() {
        super.layout();
        // 세 버튼: 가운데 정렬
        float totalW = BTN_SIZE * 3 + GAP * 2;
        float startX = x + (width - totalW) / 2f;
        float btnY   = y;

        chainBtn.setRect(startX,                   btnY, BTN_SIZE, BTN_SIZE);
        skillBtn.setRect(startX + BTN_SIZE + GAP,  btnY, BTN_SIZE, BTN_SIZE);
        ultBtn  .setRect(startX + (BTN_SIZE + GAP) * 2, btnY, BTN_SIZE, BTN_SIZE);

        // 충전 버튼: 연계기 버튼 왼쪽
        artsChargeBtn.setRect(startX - BTN_SIZE - GAP, btnY, BTN_SIZE, BTN_SIZE);

        // 취소 버튼: 스킬 버튼 바로 위
        float cancelX = startX + BTN_SIZE + GAP;
        cancelBtn.setRect(cancelX, btnY - BTN_SIZE - GAP, BTN_SIZE, BTN_SIZE);
    }

    @Override
    public void update() {
        super.update();  // Group.update() → 자식 update() (클릭 감지 포함)
        if (Dungeon.hero == null) return;

        Hero hero = Dungeon.hero;
        skillBtn     .updateDisplay(hero);
        ultBtn       .updateDisplay(hero);
        chainBtn     .updateDisplay(hero);
        artsChargeBtn.updateDisplay(hero);

        boolean targeting = hero.isBattleSkillTargeting() || hero.isUltimateTargeting();
        cancelBtn.visible = targeting;
        cancelBtn.active  = targeting;
    }

    /** GameScene 외부에서 즉시 갱신이 필요할 때 호출 */
    public static void refresh() {
        if (instance != null) instance.update();
    }

    // ──────────────────────────────────────────────────────────────
    // 공통 버튼 베이스
    // ──────────────────────────────────────────────────────────────

    private static abstract class HudBtn extends Button {
        protected NinePatch        bg;
        protected RenderedTextBlock mainLabel;

        @Override
        protected void createChildren() {
            super.createChildren();
            bg = Chrome.get(Chrome.Type.GREY_BUTTON);
            addToBack(bg);

            mainLabel = PixelScene.renderTextBlock(6);
            add(mainLabel);
        }

        @Override
        protected void layout() {
            super.layout();
            bg.x = x; bg.y = y;
            bg.size(width, height);
        }

        protected void centerLabel(RenderedTextBlock lbl, float offsetY) {
            lbl.setPos(
                x + (width - lbl.width()) / 2f,
                y + (height - lbl.height()) / 2f + offsetY
            );
            PixelScene.align(lbl);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 배틀스킬 버튼
    // ──────────────────────────────────────────────────────────────

    private class SkillBtn extends HudBtn {
        private ColorBlock cooldownOverlay;

        @Override
        protected void createChildren() {
            super.createChildren();
            mainLabel.text("스킬");
            cooldownOverlay = new ColorBlock(1, 1, 0x99000000);
            add(cooldownOverlay);
        }

        void updateDisplay(Hero hero) {
            BattleSkill skill = hero.activeBattleSkill;
            if (skill == null) { visible = false; active = false; return; }
            visible = true;

            // 텍스트: 스킬명 최대 4글자
            String name = skill.name();
            mainLabel.text(name.length() > 4 ? name.substring(0, 4) : name);
            centerLabel(mainLabel, 0);

            if (skill.isReady()) {
                bg.hardlight(0.4f, 0.6f, 1.0f);
                cooldownOverlay.visible = false;
                active = !hero.isBattleSkillTargeting(); // 이미 타겟팅 중이면 비활성
            } else {
                bg.hardlight(0.2f, 0.2f, 0.2f);
                float frac = (float) skill.cooldown() / skill.baseCooldown();
                cooldownOverlay.visible = true;
                cooldownOverlay.size(width, height * frac);
                cooldownOverlay.x = x;
                cooldownOverlay.y = y;
                active = false;
            }
        }

        @Override
        protected void onClick() {
            if (Dungeon.hero == null) return;
            if (Dungeon.hero.isBattleSkillTargeting()) {
                Dungeon.hero.cancelBattleSkillTargeting();
            } else {
                Dungeon.hero.enterBattleSkillTargeting();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 궁극기 버튼
    // ──────────────────────────────────────────────────────────────

    private class UltBtn extends HudBtn {
        private ColorBlock chargeBar;

        @Override
        protected void createChildren() {
            super.createChildren();
            mainLabel.text("궁극");
            chargeBar = new ColorBlock(1, 3, 0xFFFFAA00);
            add(chargeBar);
        }

        void updateDisplay(Hero hero) {
            Ultimate ult = hero.activeUltimate;
            if (ult == null) { visible = false; active = false; return; }
            visible = true;

            String name = ult.name();
            mainLabel.text(name.length() > 4 ? name.substring(0, 4) : name);
            centerLabel(mainLabel, -2f);

            if (ult.isReady()) {
                bg.hardlight(1.0f, 0.6f, 0.1f);
                active = !hero.isUltimateTargeting();
            } else {
                bg.hardlight(0.2f, 0.2f, 0.2f);
                active = false;
            }

            // 충전 게이지 (버튼 하단)
            float frac = ult.chargePercent();
            chargeBar.size(width * frac, 3);
            chargeBar.x = x;
            chargeBar.y = y + height - 3;
        }

        @Override
        protected void onClick() {
            if (Dungeon.hero == null) return;
            if (Dungeon.hero.isUltimateTargeting()) {
                Dungeon.hero.cancelUltimateTargeting();
            } else {
                Dungeon.hero.enterUltimateTargeting();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 연계기 버튼
    // ──────────────────────────────────────────────────────────────

    private class ChainBtn extends HudBtn {
        private RenderedTextBlock timerLabel;
        private ColorBlock        timerBar;

        @Override
        protected void createChildren() {
            super.createChildren();
            mainLabel.text("연계");

            timerLabel = PixelScene.renderTextBlock(5);
            timerLabel.text("");
            add(timerLabel);

            timerBar = new ColorBlock(1, 2, 0xFF00FFAA);
            add(timerBar);
        }

        void updateDisplay(Hero hero) {
            ChainQueue.Entry entry = hero.chainQueue.peek();
            if (entry == null) { visible = false; active = false; return; }
            visible = true;
            active  = true;

            // 연계기 이름
            String chainName = entry.operator.chainName();
            mainLabel.text(chainName.length() > 4 ? chainName.substring(0, 4) : chainName);
            centerLabel(mainLabel, -3f);

            // 남은 시간
            float remaining = Math.max(0f, entry.expiresAt - Actor.now());
            timerLabel.text(String.format("%.1f", remaining));
            timerLabel.setPos(
                x + (width - timerLabel.width()) / 2f,
                y + height - timerLabel.height() - 3
            );
            PixelScene.align(timerLabel);

            // 타이머 바 (남은 비율)
            float frac = remaining / ChainQueue.DEFAULT_WINDOW;
            frac = Math.min(1f, Math.max(0f, frac));
            timerBar.size(width * frac, 2);
            timerBar.x = x;
            timerBar.y = y + height - 2;

            bg.hardlight(0.1f, 0.7f, 0.5f);
        }

        @Override
        protected void onClick() {
            if (Dungeon.hero == null) return;
            // 현재 공격 대상을 연계기에 전달 (없으면 null — 각 구현에서 처리)
            Dungeon.hero.activateFrontChain(Dungeon.hero.getAttackTarget());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 아츠유닛 충전 버튼
    // ──────────────────────────────────────────────────────────────

    private class ArtsChargeBtn extends HudBtn {
        private RenderedTextBlock chargeLabel;

        @Override
        protected void createChildren() {
            super.createChildren();
            mainLabel.text("충전");

            chargeLabel = PixelScene.renderTextBlock(6);
            chargeLabel.text("");
            add(chargeLabel);
        }

        void updateDisplay(Hero hero) {
            Operator op = hero.activeMainOperator;
            // 아츠유닛이 아니거나 충전이 없으면 숨김
            if (op == null
                    || op.weaponType() != Operator.WeaponType.ARTS_UNIT
                    || op.getArtsCharges() <= 0) {
                visible = false;
                active  = false;
                return;
            }
            visible = true;
            active  = true;

            // 충전 개수 표시
            int charges = op.getArtsCharges();
            chargeLabel.text(Integer.toString(charges));
            chargeLabel.setPos(
                x + (width - chargeLabel.width()) / 2f,
                y + height - chargeLabel.height() - 2
            );
            PixelScene.align(chargeLabel);

            bg.hardlight(0.3f, 0.8f, 1.0f);
            centerLabel(mainLabel, -3f);
        }

        @Override
        protected void onClick() {
            Hero hero = Dungeon.hero;
            if (hero == null) return;
            hero.curAction = new HeroAction.UseArtsCharge();
            GameScene.ready();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 타겟팅 취소 버튼
    // ──────────────────────────────────────────────────────────────

    private class CancelBtn extends HudBtn {

        @Override
        protected void createChildren() {
            super.createChildren();
            mainLabel.text("X");
            bg.hardlight(1.0f, 0.2f, 0.2f);
        }

        @Override
        protected void layout() {
            super.layout();
            centerLabel(mainLabel, 0);
        }

        @Override
        protected void onClick() {
            if (Dungeon.hero == null) return;
            if (Dungeon.hero.isBattleSkillTargeting()) {
                Dungeon.hero.cancelBattleSkillTargeting();
            } else if (Dungeon.hero.isUltimateTargeting()) {
                Dungeon.hero.cancelUltimateTargeting();
            }
        }
    }
}
