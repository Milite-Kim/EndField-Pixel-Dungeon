/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.operators.BattleSkill;
import com.shatteredpixel.shatteredpixeldungeon.operators.Operator;
import com.shatteredpixel.shatteredpixeldungeon.operators.TeamOperator;
import com.shatteredpixel.shatteredpixeldungeon.operators.Ultimate;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/**
 * 오퍼레이터 정보 팝업.
 *
 * 오퍼레이터 선택 화면의 '정보' 버튼으로 열리며,
 * 직군/무기/속성과 배틀스킬·연계기·궁극기의 이름·설명을 보여준다.
 */
public class WndOperatorInfo extends Window {

    private static final int WIDTH = 160;
    private static final int GAP   = 3;

    public WndOperatorInfo(Operator op) {
        super();

        float pos = 0;

        // ── 이름 ──────────────────────────────────
        RenderedTextBlock title = PixelScene.renderTextBlock(op.name(), 9);
        title.hardlight(TITLE_COLOR);
        title.maxWidth(WIDTH);
        title.setPos(0, pos);
        add(title);
        pos = title.bottom() + GAP;

        // ── 직군 · 무기 · 속성 ─────────────────────
        RenderedTextBlock meta = PixelScene.renderTextBlock(
                classLabel(op.operatorClass()) + " · "
                        + weaponLabel(op.weaponType()) + " · "
                        + attrLabel(op.attribute()), 6);
        meta.hardlight(0xAABBAA);
        meta.maxWidth(WIDTH);
        meta.setPos(0, pos);
        add(meta);
        pos = meta.bottom() + GAP * 2;

        // ── 배틀스킬 ───────────────────────────────
        BattleSkill skill = op.battleSkill();
        if (skill != null) {
            pos = addSection(pos, "배틀스킬 — " + skill.name(), skill.description());
        }

        // ── 연계기 (팀 오퍼레이터 전용) ──────────────
        if (op instanceof TeamOperator) {
            TeamOperator teamOp = (TeamOperator) op;
            pos = addSection(pos, "연계기 — " + teamOp.chainName(), teamOp.chainDescription());
        }

        // ── 궁극기 ─────────────────────────────────
        Ultimate ult = op.ultimate();
        if (ult != null) {
            pos = addSection(pos, "궁극기 — " + ult.name(), ult.description());
        }

        resize(WIDTH, (int) Math.ceil(pos));
    }

    /** 섹션(헤더 + 본문)을 추가하고 다음 y 좌표를 반환한다. */
    private float addSection(float pos, String header, String body) {
        RenderedTextBlock head = PixelScene.renderTextBlock(header, 7);
        head.hardlight(0xCCFFCC);
        head.maxWidth(WIDTH);
        head.setPos(0, pos);
        add(head);
        pos = head.bottom() + 1;

        if (body != null && !body.isEmpty()) {
            RenderedTextBlock text = PixelScene.renderTextBlock(body, 6);
            text.maxWidth(WIDTH);
            text.setPos(0, pos);
            add(text);
            pos = text.bottom();
        }
        return pos + GAP * 2;
    }

    // ─────────────────────────────────────────────
    // 표시용 라벨
    // ─────────────────────────────────────────────

    private static String classLabel(Operator.OperatorClass c) {
        if (c == null) return "?";
        switch (c) {
            case STRIKER:   return "스트라이커";
            case GUARD:     return "가드";
            case CASTER:    return "캐스터";
            case SUPPORTER: return "서포터";
            case DEFENDER:  return "디펜더";
            case VANGUARD:  return "뱅가드";
            default:        return "?";
        }
    }

    private static String weaponLabel(Operator.WeaponType w) {
        if (w == null) return "?";
        switch (w) {
            case ONE_HANDED_SWORD: return "한손검";
            case TWO_HANDED_SWORD: return "양손검";
            case POLEARM:          return "장병기";
            case HANDGUN:          return "권총";
            case ARTS_UNIT:        return "아츠유닛";
            default:               return "?";
        }
    }

    private static String attrLabel(Operator.Attribute a) {
        if (a == null) return "?";
        switch (a) {
            case PHYSICAL: return "물리";
            case HEAT:     return "열기";
            case COLD:     return "냉기";
            case NATURE:   return "자연";
            case ELECTRIC: return "전기";
            default:       return "?";
        }
    }
}
