/*
 * EndField Pixel Dungeon
 * Based on Shattered Pixel Dungeon by Evan Debenham
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 연타 (FollowUp)
 *
 * 여풍(Yeofung)의 연계기로 메인 오퍼레이터(Hero)에게 부여되는 버프.
 *
 * 효과: 다음 배틀스킬 또는 궁극기 발동 시 소모 → 추가 피해 1회
 * - 스택 수만큼 추가 타격 기회를 저장 (현재 최대 1스택)
 *
 * 소모 시점: 각 오퍼레이터의 배틀스킬/궁극기 activate() 내에서 직접 호출
 *
 * TODO: 추가 피해 배율 수치 확정
 */
public class FollowUp extends Buff {

    /** 연타 추가 피해 배율. TODO: 수치 확정 */
    public static final float BONUS_DMG_MULT = 1.0f;

    private int stacks = 1;

    {
        type = buffType.POSITIVE;
        announced = true;
    }

    // ─────────────────────────────────────────────
    // 적용 (정적 진입점)
    // ─────────────────────────────────────────────

    /** 메인 오퍼레이터(Hero)에게 연타 버프를 부여한다. 이미 있으면 갱신. */
    public static void apply(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero) {
        FollowUp fu = Buff.affect(hero, FollowUp.class);
        fu.stacks = 1; // 현재 최대 1스택, 중첩 시 갱신만
    }

    // ─────────────────────────────────────────────
    // 소모 (배틀스킬/궁극기 activate 시 호출)
    // ─────────────────────────────────────────────

    /**
     * 연타 버프를 1스택 소모한다.
     * 스택이 남아있었으면 true 반환 (추가 피해 처리해야 함).
     * 스택 소모 후 0이 되면 버프 제거.
     */
    public boolean consume() {
        if (stacks <= 0) {
            detach();
            return false;
        }
        stacks--;
        if (stacks <= 0) detach();
        return true;
    }

    public int stacks() { return stacks; }

    @Override
    public int icon() {
        return BuffIndicator.FURY; // TODO: 전용 아이콘 추가
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
