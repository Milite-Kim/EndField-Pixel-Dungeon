/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 와류 (Whirlpool) — 탕탕 연계기 / 궁극기 스택 버프
 *
 * 탕탕 연계기 적중 시 +1스택 (최대 MAX_STACKS).
 * 탕탕 궁극기 발동 시 +1스택.
 * 탕탕 배틀스킬 사용 시: 와류 전량 소모 → 소모량 비례 추가 냉기 피해 + 아츠 취약.
 *
 * Hero 버프로 유지 (탕탕이 파티에서 제거될 때까지 지속).
 */
public class Whirlpool extends Buff {

    { type = buffType.POSITIVE; announced = true; }

    /** 최대 스택 수. */
    public static final int MAX_STACKS = 2;

    private int stacks = 0;

    // ─────────────────────────────────────────────
    // 스택 조작
    // ─────────────────────────────────────────────

    public int stacks() { return stacks; }

    /**
     * 스택 1 추가 (최대 MAX_STACKS).
     */
    public void addStack() {
        stacks = Math.min(stacks + 1, MAX_STACKS);
    }

    /**
     * 스택 전량 소모.
     * @return 소모된 스택 수
     */
    public int consumeAll() {
        int consumed = stacks;
        stacks = 0;
        return consumed;
    }

    // ─────────────────────────────────────────────
    // 버프 유지 (영구)
    // ─────────────────────────────────────────────

    @Override
    public boolean act() {
        spend(TICK);
        return true;
    }

    @Override
    public int icon() {
        return BuffIndicator.NONE; // TODO: 와류 아이콘
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(stacks);
    }

    // ─────────────────────────────────────────────
    // 저장/불러오기
    // ─────────────────────────────────────────────

    private static final String STACKS = "stacks";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(STACKS, stacks);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        stacks = bundle.getInt(STACKS);
    }
}
